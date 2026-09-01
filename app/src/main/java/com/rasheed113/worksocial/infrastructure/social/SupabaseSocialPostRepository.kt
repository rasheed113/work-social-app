package com.rasheed113.worksocial.infrastructure.social

import com.rasheed113.worksocial.domain.social.CommentsResult
import com.rasheed113.worksocial.domain.social.CreateCommentResult
import com.rasheed113.worksocial.domain.social.CreatePostAttachment
import com.rasheed113.worksocial.domain.social.CreatePostLocation
import com.rasheed113.worksocial.domain.social.CreatePostResult
import com.rasheed113.worksocial.domain.social.DeleteCommentResult
import com.rasheed113.worksocial.domain.social.LikeMutationResult
import com.rasheed113.worksocial.domain.social.SocialComment
import com.rasheed113.worksocial.domain.social.SocialCommentAuthor
import com.rasheed113.worksocial.domain.social.SocialPost
import com.rasheed113.worksocial.domain.social.SocialPostAuthor
import com.rasheed113.worksocial.domain.social.SocialPostMedia
import com.rasheed113.worksocial.domain.social.SocialPostRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable internal data class PostAuthorDto(val username: String, val display_name: String, val avatar_url: String? = null)
@Serializable internal data class PostDto(val id: String, val profile_id: String, val content: String, val privacy: String, val latitude: Double? = null, val longitude: Double? = null, val location_name: String? = null, val created_at: String, val updated_at: String, val profiles: PostAuthorDto? = null)
@Serializable internal data class PostAttachmentDto(val id: String, val post_id: String, val kind: String, val storage_path: String, val file_name: String? = null, val mime_type: String? = null, val file_size: Long? = null)
@Serializable internal data class PostReactionDto(val post_id: String, val profile_id: String)
@Serializable internal data class CreatePostPayload(val profile_id: String, val content: String, val latitude: Double? = null, val longitude: Double? = null, val location_name: String? = null)
@Serializable private data class LikePayload(val post_id: String, val profile_id: String)
@Serializable private data class CreatedPostDto(val id: String)
@Serializable private data class CreateAttachmentPayload(val post_id: String, val profile_id: String, val kind: String, val storage_path: String, val file_name: String, val mime_type: String?, val file_size: Long)
@Serializable internal data class CommentAuthorDto(val display_name: String, val avatar_url: String? = null)
@Serializable internal data class CommentDto(val id: String, val post_id: String, val profile_id: String, val parent_comment_id: String? = null, val content: String, val created_at: String, val updated_at: String, val profiles: CommentAuthorDto? = null)
@Serializable private data class CreateCommentPayload(val post_id: String, val profile_id: String, val content: String)

internal fun createPostPayload(authenticatedUserId: String, content: String, location: CreatePostLocation? = null) = CreatePostPayload(authenticatedUserId, content.trim(), location?.latitude, location?.longitude, location?.name)
internal fun applyLikeState(posts: List<SocialPost>, reactions: List<PostReactionDto>, currentUserId: String): List<SocialPost> {
    val likesByPost = reactions.groupingBy(PostReactionDto::post_id).eachCount()
    val likedByCurrentUser = reactions.asSequence().filter { it.profile_id == currentUserId }.map(PostReactionDto::post_id).toSet()
    return posts.map { it.copy(likeCount = likesByPost[it.id] ?: 0, isLikedByCurrentUser = it.id in likedByCurrentUser) }
}
internal fun mapComment(dto: CommentDto, currentUserId: String = ""): SocialComment {
    val profile = requireNotNull(dto.profiles) { "Comment ${dto.id} is missing its author profile relation." }
    return SocialComment(dto.id, dto.post_id, dto.profile_id, dto.parent_comment_id, dto.content, dto.created_at, dto.updated_at, SocialCommentAuthor(profile.display_name, profile.avatar_url), dto.profile_id == currentUserId)
}
internal object SocialPostMapper {
    fun map(post: PostDto, attachments: List<PostAttachmentDto>, publicUrl: (String) -> String): SocialPost {
        val profile = requireNotNull(post.profiles) { "Post ${post.id} is missing its author profile relation." }
        return SocialPost(post.id, post.profile_id, post.content, post.privacy, post.latitude, post.longitude, post.location_name, post.created_at, post.updated_at, SocialPostAuthor(profile.username, profile.display_name, profile.avatar_url), attachments.map { a -> SocialPostMedia(a.id, a.kind, a.storage_path, a.file_name, a.mime_type, a.file_size, publicUrl(a.storage_path)) })
    }
}

class SupabaseSocialPostRepository(private val postgrest: Postgrest, private val auth: Auth, private val storage: Storage) : SocialPostRepository {
    companion object { const val INITIAL_PAGE_SIZE = 50 }

    override suspend fun getHomePosts(): List<SocialPost> {
        val userId = requireActiveSession()
        val posts = postgrest.from("posts").select(columns = Columns.raw("id, profile_id, content, privacy, latitude, longitude, location_name, created_at, updated_at, profiles(username, display_name, avatar_url)")) { filter { eq("privacy", "public") }; order(column = "created_at", order = Order.DESCENDING); limit(INITIAL_PAGE_SIZE.toLong()) }.decodeList<PostDto>()
        return hydratePosts(posts, userId)
    }

    override suspend fun getProfilePosts(profileId: String): List<SocialPost> {
        val userId = requireActiveSession()
        if (profileId.isBlank()) return emptyList()
        val posts = postgrest.from("posts").select(columns = Columns.raw("id, profile_id, content, privacy, latitude, longitude, location_name, created_at, updated_at, profiles(username, display_name, avatar_url)")) { filter { eq("profile_id", profileId) }; order(column = "created_at", order = Order.DESCENDING); limit(INITIAL_PAGE_SIZE.toLong()) }.decodeList<PostDto>()
        return hydratePosts(posts, userId)
    }

    private suspend fun hydratePosts(posts: List<PostDto>, userId: String): List<SocialPost> {
        if (posts.isEmpty()) return emptyList()
        val ids = posts.map(PostDto::id)
        val attachments = postgrest.from("post_attachments").select(columns = Columns.list("id, post_id, kind, storage_path, file_name, mime_type, file_size")) { filter { isIn("post_id", ids) }; order(column = "created_at", order = Order.ASCENDING) }.decodeList<PostAttachmentDto>()
        val likes = postgrest.from("likes").select(columns = Columns.list("post_id, profile_id")) { filter { isIn("post_id", ids) } }.decodeList<PostReactionDto>()
        val byPost = attachments.groupBy(PostAttachmentDto::post_id)
        val publicUrl: (String) -> String = { storage.from("post-media").publicUrl(it) }
        return applyLikeState(posts.map { SocialPostMapper.map(it, byPost[it.id].orEmpty(), publicUrl) }, likes, userId)
    }

    override suspend fun createPost(content: String, attachments: List<CreatePostAttachment>, location: CreatePostLocation?): CreatePostResult {
        val userId = auth.currentSessionOrNull()?.user?.id ?: return CreatePostResult.Failure("Your Work Social session is no longer active. Please sign in again.")
        val normalizedContent = content.trim()
        if (normalizedContent.isEmpty() && attachments.isEmpty() && location == null) return CreatePostResult.Failure("Post cannot be empty.")
        val postId = runCatching { postgrest.from("posts").insert(createPostPayload(userId, normalizedContent, location)) { select(columns = Columns.list("id")) }.decodeSingle<CreatedPostDto>().id }
            .getOrElse { return CreatePostResult.Failure(it.message?.takeIf(String::isNotBlank) ?: "Unable to create your post right now.") }
        val uploadedPaths = mutableListOf<String>()
        try {
            for (attachment in attachments) {
                val safeName = attachment.fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val storagePath = "$userId/$postId/${UUID.randomUUID()}-$safeName"
                storage.from("post-media").upload(storagePath, attachment.bytes) { upsert = false }
                uploadedPaths += storagePath
                postgrest.from("post_attachments").insert(CreateAttachmentPayload(postId, userId, attachment.kind, storagePath, attachment.fileName, attachment.mimeType, attachment.fileSize))
            }
            return CreatePostResult.Created(postId)
        } catch (error: Throwable) {
            if (uploadedPaths.isNotEmpty()) runCatching { storage.from("post-media").delete(*uploadedPaths.toTypedArray()) }
            runCatching { postgrest.from("post_attachments").delete { filter { eq("post_id", postId) } } }
            runCatching { postgrest.from("posts").delete { filter { eq("id", postId); eq("profile_id", userId) } } }
            return CreatePostResult.Failure(error.message?.takeIf(String::isNotBlank) ?: "Unable to upload your post media right now.")
        }
    }

    override suspend fun likePost(postId: String) = setLike(postId, true)
    override suspend fun unlikePost(postId: String) = setLike(postId, false)

    private suspend fun setLike(postId: String, liked: Boolean): LikeMutationResult {
        val userId = auth.currentSessionOrNull()?.user?.id ?: return LikeMutationResult.Failure("Your Work Social session is no longer active. Please sign in again.")
        if (postId.isBlank()) return LikeMutationResult.Failure("Post ID cannot be empty.")
        return runCatching {
            if (liked) {
                postgrest.from("likes").upsert(LikePayload(postId, userId)) { onConflict = "post_id,profile_id" }
            } else {
                postgrest.from("likes").delete { filter { eq("post_id", postId); eq("profile_id", userId) } }
            }
            val likes = postgrest.from("likes").select(columns = Columns.list("post_id, profile_id")) { filter { eq("post_id", postId) } }.decodeList<PostReactionDto>()
            LikeMutationResult.Success(likes.size, likes.any { it.profile_id == userId })
        }.getOrElse { LikeMutationResult.Failure(it.message?.takeIf(String::isNotBlank) ?: "Unable to update this like right now.") }
    }

    override suspend fun getComments(postId: String): CommentsResult {
        val userId = auth.currentSessionOrNull()?.user?.id ?: return CommentsResult.Failure("Your Work Social session is no longer active. Please sign in again.")
        if (postId.isBlank()) return CommentsResult.Failure("Post ID cannot be empty.")
        return runCatching {
            val rows = postgrest.from("post_comments").select(columns = Columns.raw("id, post_id, profile_id, parent_comment_id, content, created_at, updated_at, profiles(display_name, avatar_url)")) { filter { eq("post_id", postId) }; order(column = "created_at", order = Order.ASCENDING) }.decodeList<CommentDto>()
            CommentsResult.Success(rows.map { mapComment(it, userId) })
        }.getOrElse { CommentsResult.Failure(it.message?.takeIf(String::isNotBlank) ?: "Unable to load comments right now.") }
    }

    override suspend fun createComment(postId: String, content: String): CreateCommentResult {
        val userId = auth.currentSessionOrNull()?.user?.id ?: return CreateCommentResult.Failure("Your Work Social session is no longer active. Please sign in again.")
        val normalized = content.trim()
        if (postId.isBlank()) return CreateCommentResult.Failure("Post ID cannot be empty.")
        if (normalized.isEmpty()) return CreateCommentResult.Failure("Comment cannot be empty.")
        return runCatching { CreateCommentResult.Created(postgrest.from("post_comments").insert(CreateCommentPayload(postId, userId, normalized)) { select(columns = Columns.list("id")) }.decodeSingle<CreatedPostDto>().id) }.getOrElse { CreateCommentResult.Failure(it.message?.takeIf(String::isNotBlank) ?: "Unable to create your comment right now.") }
    }

    override suspend fun deleteComment(commentId: String): DeleteCommentResult {
        val userId = auth.currentSessionOrNull()?.user?.id ?: return DeleteCommentResult.Failure("Your Work Social session is no longer active. Please sign in again.")
        if (commentId.isBlank()) return DeleteCommentResult.Failure("Comment ID cannot be empty.")
        return runCatching { postgrest.from("post_comments").delete { filter { eq("id", commentId); eq("profile_id", userId) } }; DeleteCommentResult.Deleted }.getOrElse { DeleteCommentResult.Failure(it.message?.takeIf(String::isNotBlank) ?: "Unable to delete your comment right now.") }
    }

    private fun requireActiveSession(): String = auth.currentSessionOrNull()?.user?.id ?: error("Your Work Social session is no longer active. Please sign in again.")
}
