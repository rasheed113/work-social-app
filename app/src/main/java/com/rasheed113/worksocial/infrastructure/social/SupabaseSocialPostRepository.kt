package com.rasheed113.worksocial.infrastructure.social

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
        check(auth.currentSessionOrNull() != null) {
            "Your Work Social session is no longer active. Please sign in again."
        }

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

        val attachmentsByPost = attachments.groupBy(PostAttachmentDto::post_id)
        val publicUrl: (String) -> String = { path -> storage.from("post-media").publicUrl(path) }

        return posts.map { post ->
            SocialPostMapper.map(post, attachmentsByPost[post.id].orEmpty(), publicUrl)
        }
    }
}
