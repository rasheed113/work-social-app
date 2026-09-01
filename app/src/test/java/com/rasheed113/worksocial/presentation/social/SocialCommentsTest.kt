package com.rasheed113.worksocial.presentation.social

import com.rasheed113.worksocial.domain.social.*
import com.rasheed113.worksocial.infrastructure.social.CommentAuthorDto
import com.rasheed113.worksocial.infrastructure.social.CommentDto
import com.rasheed113.worksocial.infrastructure.social.mapComment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class SocialCommentsTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }
    @Test fun commentMappingPreservesBackendFieldsAndAuthor() { val mapped = mapComment(CommentDto("c1", "p1", "u1", null, "Hello", "2026-08-31T10:00:00Z", "2026-08-31T10:00:00Z", CommentAuthorDto("Rasheed", "https://avatar")), "u1"); assertTrue(mapped.isOwnedByCurrentUser); assertEquals("Rasheed", mapped.author.display_name); assertEquals("Hello", mapped.content) }
    @Test fun commentsLoadingSuccessAndEmptyRemainDistinct() = runTest { val post = samplePost(); val vm = SocialHomeViewModel(FakeRepository(posts = listOf(post), comments = CommentsResult.Success(emptyList()))); vm.load(); advanceUntilIdle(); vm.openComments(post.id); advanceUntilIdle(); val state = vm.state.value as SocialHomeState.Success; assertTrue(state.comments[post.id] is CommentsState.Success); assertEquals(emptyList<SocialComment>(), (state.comments[post.id] as CommentsState.Success).comments) }
    @Test fun commentsLoadingErrorIsNotConvertedToEmpty() = runTest { val post = samplePost(); val vm = SocialHomeViewModel(FakeRepository(posts = listOf(post), comments = CommentsResult.Failure("RLS rejected"))); vm.load(); advanceUntilIdle(); vm.openComments(post.id); advanceUntilIdle(); val state = vm.state.value as SocialHomeState.Success; assertTrue(state.comments[post.id] is CommentsState.Error); assertEquals("RLS rejected", (state.comments[post.id] as CommentsState.Error).message) }
    @Test fun createCommentRejectsWhitespaceAndDoesNotCallBackend() = runTest { val repo = FakeRepository(posts = listOf(samplePost())); val vm = SocialHomeViewModel(repo); vm.load(); advanceUntilIdle(); vm.createComment("p1", "   "); advanceUntilIdle(); assertEquals(0, repo.createCalls); assertEquals("Comment cannot be empty.", (vm.state.value as SocialHomeState.Success).actionError) }
    @Test fun createCommentPreventsDuplicateSubmissionAndReadsBack() = runTest { val gate = CompletableDeferred<Unit>(); val repo = FakeRepository(posts = listOf(samplePost()), comments = CommentsResult.Success(emptyList()), createResult = CreateCommentResult.Created("c1"), createGate = gate); val vm = SocialHomeViewModel(repo); vm.load(); advanceUntilIdle(); vm.openComments("p1"); advanceUntilIdle(); vm.createComment("p1", "Hello"); vm.createComment("p1", "Hello"); dispatcher.scheduler.runCurrent(); gate.complete(Unit); advanceUntilIdle(); assertEquals(1, repo.createCalls); assertEquals(2, repo.getCommentsCalls) }
    @Test fun createBackendFailureIsExposed() = runTest { val repo = FakeRepository(posts = listOf(samplePost()), createResult = CreateCommentResult.Failure("database unavailable")); val vm = SocialHomeViewModel(repo); vm.load(); advanceUntilIdle(); vm.createComment("p1", "Hello"); advanceUntilIdle(); assertEquals("database unavailable", (vm.state.value as SocialHomeState.Success).actionError) }
    @Test fun deleteUsesRepositoryAndReadsBack() = runTest { val repo = FakeRepository(posts = listOf(samplePost()), comments = CommentsResult.Success(emptyList()), deleteResult = DeleteCommentResult.Deleted); val vm = SocialHomeViewModel(repo); vm.load(); advanceUntilIdle(); vm.deleteComment("p1", "c1"); advanceUntilIdle(); assertEquals(1, repo.deleteCalls); assertEquals(1, repo.getCommentsCalls) }
    @Test fun deleteFailureIsExposed() = runTest { val repo = FakeRepository(posts = listOf(samplePost()), deleteResult = DeleteCommentResult.Failure("RLS rejected")); val vm = SocialHomeViewModel(repo); vm.load(); advanceUntilIdle(); vm.deleteComment("p1", "c1"); advanceUntilIdle(); assertEquals("RLS rejected", (vm.state.value as SocialHomeState.Success).actionError) }
    private fun samplePost() = SocialPost("p1", "author", "post", "public", created_at = "2026-08-31T10:00:00Z", updated_at = "2026-08-31T10:00:00Z", author = SocialPostAuthor("user", "User"))
    private class FakeRepository(private val posts: List<SocialPost>, private val comments: CommentsResult = CommentsResult.Success(emptyList()), private val createResult: CreateCommentResult = CreateCommentResult.Created("c1"), private val deleteResult: DeleteCommentResult = DeleteCommentResult.Deleted, private val createGate: CompletableDeferred<Unit>? = null) : SocialPostRepository { var createCalls = 0; var deleteCalls = 0; var getCommentsCalls = 0; override suspend fun getHomePosts() = posts; override suspend fun createPost(content: String) = CreatePostResult.Failure("unused"); override suspend fun likePost(postId: String) = LikeMutationResult.Failure("unused"); override suspend fun unlikePost(postId: String) = LikeMutationResult.Failure("unused"); override suspend fun getComments(postId: String): CommentsResult { getCommentsCalls++; return comments }; override suspend fun createComment(postId: String, content: String): CreateCommentResult { createCalls++; createGate?.await(); return createResult }; override suspend fun deleteComment(commentId: String): DeleteCommentResult { deleteCalls++; return deleteResult } }
}
