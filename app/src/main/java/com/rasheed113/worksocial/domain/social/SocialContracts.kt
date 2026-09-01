package com.rasheed113.worksocial.domain.social

import kotlinx.serialization.Serializable

@Serializable data class SocialPostAuthor(val username: String, val display_name: String, val avatar_url: String? = null)
@Serializable data class SocialPostMedia(val id: String, val kind: String, val storage_path: String, val file_name: String? = null, val mime_type: String? = null, val file_size: Long? = null, val public_url: String)
@Serializable data class SocialPost(val id: String, val profile_id: String, val content: String, val privacy: String, val latitude: Double? = null, val longitude: Double? = null, val location_name: String? = null, val created_at: String, val updated_at: String, val author: SocialPostAuthor, val media: List<SocialPostMedia> = emptyList(), val likeCount: Int = 0, val isLikedByCurrentUser: Boolean = false)
@Serializable data class SocialCommentAuthor(val display_name: String, val avatar_url: String? = null)
@Serializable data class SocialComment(val id: String, val postId: String, val profileId: String, val parentCommentId: String? = null, val content: String, val createdAt: String, val updatedAt: String, val author: SocialCommentAuthor, val isOwnedByCurrentUser: Boolean = false)

data class CreatePostAttachment(
    val fileName: String,
    val mimeType: String?,
    val bytes: ByteArray,
    val kind: String,
) {
    val fileSize: Long get() = bytes.size.toLong()
}

data class CreatePostLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
)

sealed interface CreatePostResult { data class Created(val postId: String) : CreatePostResult; data class Failure(val message: String) : CreatePostResult }
sealed interface LikeMutationResult { data class Success(val likeCount: Int, val isLikedByCurrentUser: Boolean) : LikeMutationResult; data class Failure(val message: String) : LikeMutationResult }
sealed interface CreateCommentResult { data class Created(val commentId: String) : CreateCommentResult; data class Failure(val message: String) : CreateCommentResult }
sealed interface DeleteCommentResult { data object Deleted : DeleteCommentResult; data class Failure(val message: String) : DeleteCommentResult }
sealed interface CommentsResult { data class Success(val comments: List<SocialComment>) : CommentsResult; data class Failure(val message: String) : CommentsResult }
interface SocialPostRepository {
    suspend fun getHomePosts(): List<SocialPost>
    suspend fun getProfilePosts(profileId: String) = emptyList<SocialPost>()
    suspend fun createPost(content: String, attachments: List<CreatePostAttachment> = emptyList(), location: CreatePostLocation? = null): CreatePostResult
    suspend fun likePost(postId: String): LikeMutationResult
    suspend fun unlikePost(postId: String): LikeMutationResult
    suspend fun getComments(postId: String): CommentsResult = CommentsResult.Failure("Comments are not implemented by this repository.")
    suspend fun createComment(postId: String, content: String): CreateCommentResult = CreateCommentResult.Failure("Comments are not implemented by this repository.")
    suspend fun deleteComment(commentId: String): DeleteCommentResult = DeleteCommentResult.Failure("Comments are not implemented by this repository.")
}
sealed interface SocialHomeState { data object Loading : SocialHomeState; data class Success(val posts: List<SocialPost>, val likingPostIds: Set<String> = emptySet(), val actionError: String? = null, val comments: Map<String, CommentsState> = emptyMap(), val commentMutations: Set<String> = emptySet()) : SocialHomeState; data object Empty : SocialHomeState; data class Error(val message: String) : SocialHomeState }
sealed interface CommentsState { data object Loading : CommentsState; data class Success(val comments: List<SocialComment>) : CommentsState; data class Error(val message: String) : CommentsState }
