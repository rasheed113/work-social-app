package com.rasheed113.worksocial

import com.rasheed113.worksocial.domain.social.CreatePostResult
import com.rasheed113.worksocial.domain.social.LikeMutationResult
import com.rasheed113.worksocial.domain.social.SocialHomeState
import com.rasheed113.worksocial.domain.social.SocialPost
import com.rasheed113.worksocial.domain.social.SocialPostAuthor
import com.rasheed113.worksocial.domain.social.SocialPostRepository
import com.rasheed113.worksocial.infrastructure.social.PostAttachmentDto
import com.rasheed113.worksocial.infrastructure.social.PostAuthorDto
import com.rasheed113.worksocial.infrastructure.social.PostDto
import com.rasheed113.worksocial.infrastructure.social.SocialPostMapper
import com.rasheed113.worksocial.presentation.social.SocialHomeViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SocialHomeTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun postMappingPreservesRealPostAndAuthorContract() {
        val dto = PostDto(
            id = "post-1",
            profile_id = "profile-1",
            content = "Real post",
            privacy = "public",
            created_at = "2026-08-31T10:00:00Z",
            updated_at = "2026-08-31T10:00:00Z",
            profiles = PostAuthorDto("rasheed", "Rasheed", "https://example/avatar.jpg"),
        )
        val attachment = PostAttachmentDto(
            id = "attachment-1",
            post_id = "post-1",
            kind = "image",
            storage_path = "profile-1/post-1/image.jpg",
            file_name = "image.jpg",
            mime_type = "image/jpeg",
            file_size = 42,
        )

        val mapped = SocialPostMapper.map(dto, listOf(attachment)) { "https://storage.example/image.jpg" }

        assertEquals("post-1", mapped.id)
        assertEquals("profile-1", mapped.profile_id)
        assertEquals("Rasheed", mapped.author.display_name)
        assertEquals("rasheed", mapped.author.username)
        assertEquals("https://storage.example/image.jpg", mapped.media.single().public_url)
    }

    @Test
    fun successfulRepositoryResultBecomesSuccess() = runTest {
        val post = samplePost()
        val viewModel = SocialHomeViewModel(FakeRepository(result = Result.success(listOf(post))))

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is SocialHomeState.Success)
        assertEquals(listOf(post), (state as SocialHomeState.Success).posts)
    }

    @Test
    fun emptyRepositoryResultBecomesEmpty() = runTest {
        val viewModel = SocialHomeViewModel(FakeRepository(result = Result.success(emptyList())))

        viewModel.load()
        advanceUntilIdle()

        assertEquals(SocialHomeState.Empty, viewModel.state.value)
    }

    @Test
    fun backendErrorRemainsErrorAndIsNotConvertedToEmpty() = runTest {
        val viewModel = SocialHomeViewModel(FakeRepository(result = Result.failure(IllegalStateException("database unavailable"))))

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is SocialHomeState.Error)
        assertTrue((state as SocialHomeState.Error).message.contains("database unavailable"))
    }

    @Test
    fun loadingStateIsExposedWhileBackendRequestIsPending() = runTest {
        val gate = CompletableDeferred<List<SocialPost>>()
        val viewModel = SocialHomeViewModel(object : SocialPostRepository {
            override suspend fun getHomePosts(): List<SocialPost> = gate.await()
            override suspend fun createPost(content: String): CreatePostResult =
                CreatePostResult.Failure("not used by this test")
            override suspend fun likePost(postId: String): LikeMutationResult =
                LikeMutationResult.Failure("not used by this test")
            override suspend fun unlikePost(postId: String): LikeMutationResult =
                LikeMutationResult.Failure("not used by this test")
        })

        viewModel.load()
        dispatcher.scheduler.runCurrent()

        assertEquals(SocialHomeState.Loading, viewModel.state.value)
        gate.complete(emptyList())
        advanceUntilIdle()
    }

    private fun samplePost() = SocialPost(
        id = "post-1",
        profile_id = "profile-1",
        content = "Real post",
        privacy = "public",
        created_at = "2026-08-31T10:00:00Z",
        updated_at = "2026-08-31T10:00:00Z",
        author = SocialPostAuthor("rasheed", "Rasheed"),
    )

    private class FakeRepository(private val result: Result<List<SocialPost>>) : SocialPostRepository {
        override suspend fun getHomePosts(): List<SocialPost> = result.getOrThrow()
        override suspend fun createPost(content: String): CreatePostResult =
            CreatePostResult.Failure("not used by this test")
        override suspend fun likePost(postId: String): LikeMutationResult =
            LikeMutationResult.Failure("not used by this test")
        override suspend fun unlikePost(postId: String): LikeMutationResult =
            LikeMutationResult.Failure("not used by this test")
    }
}