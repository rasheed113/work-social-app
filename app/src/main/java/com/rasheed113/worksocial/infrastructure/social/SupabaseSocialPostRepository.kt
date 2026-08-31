package com.rasheed113.worksocial.infrastructure.social

import com.rasheed113.worksocial.domain.social.CreatePostResult
import com.rasheed113.worksocial.domain.social.LikeMutationResult
import com.rasheed113.worksocial.domain.social.SocialPost
import com.rasheed113.worksocial.domain.social.SocialPostAuthor
import com.rasheed113.worksocial.domain.social.SocialPostMedia
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.Serializable

@Serializable
internal data class PostAuthorDto(
    val username: String,
    val display_name: String,
    val avatar_url: String? = null,
)

@Serializable
internal data class PostDto(
    val id: String,
    val profile_id: String,
    val content: String,
    val privacy: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val location_name: String? = null,
    val created_at: String,
    val updated_at: String,
    val profiles: PostAuthorDto? = null,
)

@Serializable
internal data class PostAttachmentDto(
    val id: String,
    val post_id: String,
    val kind: String,
    val storage_path: String,
    val file_name: String? = null,
    val mime_type: String? = null,
    val file_size: Long? = null,
)

@Serializable
internal data class PostReactionDto(
    val post_id: String,
    val profile_id: String,
    val reaction: String,
)

@Serializable
internal data class CreatePostPayload(
    val profile_id: String,
    val content: String,
)

@Serializable
private data class LikePayload(
    val post_id: String,
    val profile_id: String,
    val reaction: String,
)

@Serializable
private data class CreatedPostDto(
    val id: String,
)

internal fun createPostPayload(authenticatedUserId: String, content: String): CreatePostPayload =
    CreatePostPayload(
        profile_id = authenticatedUserId,
        content = content.trim(),
    )

internal fun applyLikeState(
    posts: List<SocialPost>,
    reactions: List<PostReactionDto>,
    currentUserId: String,
): List<SocialPost> {
    val likesByPost = reactions.asSequence()
        .filter { it.reaction == "like" }
        .groupingBy(PostReactionDto::post_id)
        .eachCount()
    val likedByCurrentUser = reactions.asSequence()
        .filter { it.reaction == "like" && it.profile_id == currentUserId }
        .map(PostReactionDto::post_id)
        .toSet()
    return posts.map { post ->
        post.copy(
            likeCount = likesByPost[post.id] ?: 0,
            isLikedByCurrentUser = post.id in likedByCurrentUser,
        )
    }
}

internal object SocialPostMapper {
    fun map(post: PostDto, attachments: List<PostAttachmentDto>, publicUrl: (String) -> String): SocialPost {
        val profile = requireNotNull(post.profiles) { "Post ${post.id} is missing its author profile relation." }
        return SocialPost(
            id = post.id,
            profile_id = post.profile_id,
            content = post.content,
            privacy = post.privacy,
            latitude = post.latitude,
            longitude = post.longitude,
            location_name = post.location_name,
            created_at = post.created_at,
            updated_at = post.updated_at,
            author = SocialPostAuthor(
                username = profile.username,
                display_name = profile.display_name,
                avatar_url = profile.avatar_url,
            ),
            media = attachments.map { attachment ->
                SocialPostMedia(
                    id = attachment.id,
                    kind = attachment.kind,
                    storage_path = attachment.storage_path,
                    file_name = attachment.file_name,
                    mime_type = attachment.mime_type,
                    file_size = attachment.file_size,
                    public_url = publicUrl(attachment.storage_path),
                )
            },
        )
    }
}

class SupabaseSocialPostRepository(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val storage: Storage,
) : com.rasheed113.worksocial.domain.social.SocialPostRepository {
    companion object {
        const val INITIAL_PAGE_SIZE = 50
    }

    override suspend fun getHomePosts(): List<SocialPost> {
        val userId = requireActiveSession()
        val posts = postgrest.from("posts").select(
            columns = Columns.raw(
                """
                id,
                profile_id,
                content,
                privacy,
                latitude,
                longitude,
                location_name,
                created_at,
                updated_at,
                profiles(username, display_name, avatar_url)
                """.trimIndent()
            )
        ) {
            filter { eq("privacy", "public") }
            order(column = "created_at", order = Order.DESCENDING)
            limit(count = INITIAL_PAGE_SIZE.toLong())
        }.decodeList<PostDto>()

        if (posts.isEmpty()) return emptyList()

        val postIds = posts.map(PostDto::id)
        val attachments = postgrest.from("post_attachments").select(
            columns = Columns.list("id, post_id, kind, storage_path, file_name, mime_type, file_size")
        ) {
            filter { isIn("post_id", postIds) }
            order(column = "created_at", order = Order.ASCENDING)
        }.decodeList<PostAttachmentDto>()

        val reactions = postgrest.from("post_reactions").select(
            columns = Columns.list("post_id, profile_id, reaction")
        ) {
            filter { isIn("post_id", postIds) }
        }.decodeList<PostReactionDto>()

        val attachmentsByPost = attachments.groupBy(PostAttachmentDto::post_id)
        val publicUrl: (String) -> String = { path -> storage.from("post-media").publicUrl(path) }
        val basePosts = posts.map { post ->
            SocialPostMapper.map(post, attachmentsByPost[post.id].orEmpty(), publicUrl)
        }
        return applyLikeState(basePosts, reactions, userId)
    }

    override suspend fun createPost(content: String): CreatePostResult {
        val userId = auth.currentSessionOrNull()?.user?.id
            ?: return CreatePostResult.Failure("Your Work Social session is no longer active. Please sign in again.")
        val payload = createPostPayload(userId, content)
        if (payload.content.isEmpty()) {
            return CreatePostResult.Failure("Post cannot be empty.")
        }

        return runCatching {
            val created = postgrest.from("posts").insert(payload) {
                select(columns = Columns.list("id"))
            }.decodeSingle<CreatedPostDto>()
            CreatePostResult.Created(created.id)
        }.getOrElse { error ->
            CreatePostResult.Failure(
                error.message?.takeIf(String::isNotBlank)
                    ?: "Unable to create your post right now.",
            )
        }
    }

    override suspend fun likePost(postId: String): LikeMutationResult = setLike(postId, true)

    override suspend fun unlikePost(postId: String): LikeMutationResult = setLike(postId, false)

    private suspend fun setLike(postId: String, liked: Boolean): LikeMutationResult {
        val userId = auth.currentSessionOrNull()?.user?.id
            ?: return LikeMutationResult.Failure("Your Work Social session is no longer active. Please sign in again.")
        if (postId.isBlank()) return LikeMutationResult.Failure("Post ID cannot be empty.")

        return runCatching {
            if (liked) {
                postgrest.from("post_reactions").upsert(
                    LikePayload(post_id = postId, profile_id = userId, reaction = "like")
                ) {
                    onConflict = "post_id,profile_id"
                }
            } else {
                postgrest.from("post_reactions").delete {
                    filter {
                        eq("post_id", postId)
                        eq("profile_id", userId)
                    }
                }
            }

            val reactions = postgrest.from("post_reactions").select(
                columns = Columns.list("post_id, profile_id, reaction")
            ) {
                filter { eq("post_id", postId) }
            }.decodeList<PostReactionDto>()

            LikeMutationResult.Success(
                likeCount = reactions.count { it.reaction == "like" },
                isLikedByCurrentUser = reactions.any {
                    it.profile_id == userId && it.reaction == "like"
                },
            )
        }.getOrElse { error ->
            LikeMutationResult.Failure(
                error.message?.takeIf(String::isNotBlank)
                    ?: if (liked) "Unable to like this post right now." else "Unable to unlike this post right now.",
            )
        }
    }

    private fun requireActiveSession(): String =
        auth.currentSessionOrNull()?.user?.id
            ?: error("Your Work Social session is no longer active. Please sign in again.")
}
