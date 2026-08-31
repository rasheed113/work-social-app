package com.rasheed113.worksocial.presentation.social

import com.rasheed113.worksocial.domain.social.CreatePostResult
import com.rasheed113.worksocial.domain.social.SocialPost
import com.rasheed113.worksocial.domain.social.SocialPostRepository
import com.rasheed113.worksocial.infrastructure.social.createPostPayload
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreatePostViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun emptyContentProducesValidationErrorWithoutRepositoryCall() = runTest {
        val repository = FakeSocialPostRepository()
        val viewModel = CreatePostViewModel(repository)

        viewModel.submit()

        assertEquals(
            CreatePostState.ValidationError("", "Post cannot be empty."),
            viewModel.state.value,
        )
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun successfulCreateProducesSuccessState() = runTest {
        val repository = FakeSocialPostRepository(
            createResult = CreatePostResult.Created("post-123"),
        )
        val viewModel = CreatePostViewModel(repository)
        viewModel.onContentChanged("  Hello Work Social  ")

        viewModel.submit()

        assertEquals(CreatePostState.Success("post-123"), viewModel.state.value)
        assertEquals(listOf("Hello Work Social"), repository.createdContents)
    }

    @Test
    fun backendFailureProducesBackendErrorState() = runTest {
        val repository = FakeSocialPostRepository(
            createResult = CreatePostResult.Failure("RLS rejected the insert."),
        )
        val viewModel = CreatePostViewModel(repository)
        viewModel.onContentChanged("A real post")

        viewModel.submit()

        assertEquals(
            CreatePostState.BackendError("A real post", "RLS rejected the insert."),
            viewModel.state.value,
        )
    }

    @Test
    fun submittingStatePreventsDuplicateRequests() = runTest {
        val release = CompletableDeferred<Unit>()
        val repository = FakeSocialPostRepository(
            createResult = CreatePostResult.Created("post-456"),
            release = release,
        )
        val viewModel = CreatePostViewModel(repository)
        viewModel.onContentChanged("Only once")

        viewModel.submit()
        assertTrue(viewModel.state.value is CreatePostState.Submitting)
        viewModel.submit()
        release.complete(Unit)

        assertEquals(1, repository.createCalls)
        assertEquals(CreatePostState.Success("post-456"), viewModel.state.value)
    }

    @Test
    fun editingAfterBackendErrorClearsSubmittingAndAllowsRetry() = runTest {
        val repository = FakeSocialPostRepository(
            createResult = CreatePostResult.Failure("Network unavailable"),
        )
        val viewModel = CreatePostViewModel(repository)
        viewModel.onContentChanged("Retry me")

        viewModel.submit()
        viewModel.onContentChanged("Retry me now")

        assertEquals(CreatePostState.Editing("Retry me now"), viewModel.state.value)
    }

    @Test
    fun createPostPayloadMapsAuthenticatedUserIdAndNormalizedContent() {
        val payload = createPostPayload(
            authenticatedUserId = "authenticated-user-id",
            content = "  Real post content  ",
        )

        assertEquals("authenticated-user-id", payload.profile_id)
        assertEquals("Real post content", payload.content)
    }

    private class FakeSocialPostRepository(
        private val createResult: CreatePostResult = CreatePostResult.Created("post-default"),
        private val release: CompletableDeferred<Unit>? = null,
    ) : SocialPostRepository {
        var createCalls: Int = 0
            private set
        val createdContents = mutableListOf<String>()

        override suspend fun getHomePosts(): List<SocialPost> = emptyList()

        override suspend fun createPost(content: String): CreatePostResult {
            createCalls += 1
            createdContents += content
            release?.await()
            return createResult
        }
    }
}
