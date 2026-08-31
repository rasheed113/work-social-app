package com.rasheed113.worksocial.domain.social

import kotlinx.serialization.Serializable

@Serializable
data class SocialPostAuthor(
    val username: String,
    val display_name: String,
    val avatar_url: String? = null,
)

@Serializable
data class SocialPostMedia(
    val id: String,
    val kind: String,
    val storage_path: String,
    val file_name: String? = null,
    val mime_type: String? = null,
    val file_size: Long? = null,
    val public_url: String,
)

@Serializable
data class SocialPost(
    val id: String,
    val profile_id: String,
    val content: String,
    val privacy: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val location_name: String? = null,
    val created_at: String,
    val updated_at: String,
    val author: SocialPostAuthor,
    val media: List<SocialPostMedia> = emptyList(),
)

sealed interface CreatePostResult {
    data class Created(val postId: String) : CreatePostResult
    data class Failure(val message: String) : CreatePostResult
}

interface SocialPostRepository {
    suspend fun getHomePosts(): List<SocialPost>
    suspend fun createPost(content: String): CreatePostResult
}

sealed interface SocialHomeState {
    data object Loading : SocialHomeState
    data class Success(val posts: List<SocialPost>) : SocialHomeState
    data object Empty : SocialHomeState
    data class Error(val message: String) : SocialHomeState
}
