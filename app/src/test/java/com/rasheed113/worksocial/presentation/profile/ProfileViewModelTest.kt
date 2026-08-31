package com.rasheed113.worksocial.presentation.profile

import com.rasheed113.worksocial.domain.account.AccountProfile
import com.rasheed113.worksocial.domain.account.AccountRepository
import com.rasheed113.worksocial.domain.account.ProfileUpdateInput
import com.rasheed113.worksocial.domain.friends.FriendMutationResult
import com.rasheed113.worksocial.domain.friends.FriendProfile
import com.rasheed113.worksocial.domain.friends.FriendPerson
import com.rasheed113.worksocial.domain.friends.FriendsData
import com.rasheed113.worksocial.domain.friends.FriendsRepository
import com.rasheed113.worksocial.domain.friends.FriendsResult
import com.rasheed113.worksocial.domain.friends.RelationshipState
import com.rasheed113.worksocial.domain.social.CreateCommentResult
import com.rasheed113.worksocial.domain.social.CreatePostResult
import com.rasheed113.worksocial.domain.social.DeleteCommentResult
import com.rasheed113.worksocial.domain.social.LikeMutationResult
import com.rasheed113.worksocial.domain.social.CommentsResult
import com.rasheed113.worksocial.domain.social.SocialPost
import com.rasheed113.worksocial.domain.social.SocialPostRepository
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

class ProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val userId = "user-1"
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun currentProfileUsesAuthenticatedIdentityAndLoadsPosts() = runTest {
        val profile = profile(userId)
        val account = FakeAccountRepository(current = profile)
        val posts = FakePostRepository(listOf(post(userId)))
        val vm = ProfileViewModel(account, FakeFriendsRepository(), posts, userId, null)
        advanceUntilIdle()
        assertEquals(ProfileLoadState.Success(profile), vm.profile.value)
        assertEquals(1, vm.posts.value.posts.size)
        assertEquals(userId, account.currentProfileCalls.single())
        assertTrue(account.profileCalls.isEmpty())
    }

    @Test fun otherProfileLoadsTargetAndPosts() = runTest {
        val target = profile("user-2")
        val account = FakeAccountRepository(current = profile(userId), other = target)
        val posts = FakePostRepository(listOf(post(target.id)))
        val vm = ProfileViewModel(account, FakeFriendsRepository(), posts, userId, target.id)
        advanceUntilIdle()
        assertEquals(ProfileLoadState.Success(target), vm.profile.value)
        assertEquals(listOf(target.id), account.profileCalls)
        assertEquals(listOf(target.id), posts.requestedProfileIds)
    }

    @Test fun missingOtherProfileIsNotFoundWithoutFallback() = runTest {
        val vm = ProfileViewModel(FakeAccountRepository(current = profile(userId)), FakeFriendsRepository(), FakePostRepository(), userId, "missing-user")
        advanceUntilIdle()
        assertEquals(ProfileLoadState.NotFound, vm.profile.value)
    }

    @Test fun backendProfileErrorRemainsVisible() = runTest {
        val vm = ProfileViewModel(FakeAccountRepository(error = IllegalStateException("profile backend failure")), FakeFriendsRepository(), FakePostRepository(), userId, null)
        advanceUntilIdle()
        assertEquals(ProfileLoadState.Error("profile backend failure"), vm.profile.value)
    }

    @Test fun profilePostsBackendErrorRemainsVisible() = runTest {
        val vm = ProfileViewModel(FakeAccountRepository(current = profile(userId)), FakeFriendsRepository(), FakePostRepository(error = IllegalStateException("posts backend failure")), userId, null)
        advanceUntilIdle()
        assertEquals("posts backend failure", vm.posts.value.error)
    }

    @Test fun profileValidationRejectsBlankDisplayName() = runTest {
        val account = FakeAccountRepository(current = profile(userId))
        val vm = ProfileViewModel(account, FakeFriendsRepository(), FakePostRepository(), userId, null)
        advanceUntilIdle()
        vm.updateEdit(ProfileEditField.DisplayName, "   ")
        vm.saveProfile()
        advanceUntilIdle()
        assertEquals("Display name is required.", vm.edit.value.error)
        assertTrue(account.updateInputs.isEmpty())
    }

    @Test fun profileUpdateUsesBackendConfirmation() = runTest {
        val original = profile(userId)
        val updated = original.copy(display_name = "Updated")
        val account = FakeAccountRepository(current = original, updateResult = updated)
        val vm = ProfileViewModel(account, FakeFriendsRepository(), FakePostRepository(), userId, null)
        advanceUntilIdle()
        vm.updateEdit(ProfileEditField.DisplayName, "Updated")
        vm.saveProfile()
        advanceUntilIdle()
        assertEquals(ProfileLoadState.Success(updated), vm.profile.value)
        assertTrue(vm.edit.value.saved)
        assertEquals("Updated", account.updateInputs.single().display_name)
    }

    @Test fun profileUpdateFailureIsVisible() = runTest {
        val account = FakeAccountRepository(current = profile(userId), updateError = IllegalStateException("update failed"))
        val vm = ProfileViewModel(account, FakeFriendsRepository(), FakePostRepository(), userId, null)
        advanceUntilIdle()
        vm.updateEdit(ProfileEditField.Bio, "New bio")
        vm.saveProfile()
        advanceUntilIdle()
        assertEquals("update failed", vm.edit.value.error)
        assertTrue(!vm.edit.value.saved)
    }

    @Test fun otherProfileRelationshipUsesPhase8Repository() = runTest {
        val target = profile("user-2")
        val friends = FakeFriendsRepository(FriendsData(listOf(FriendPerson(FriendProfile(target.id, target.username, target.display_name, target.avatar_url), RelationshipState.NONE)), emptyList(), emptyList()))
        val vm = ProfileViewModel(FakeAccountRepository(current = profile(userId), other = target), friends, FakePostRepository(), userId, target.id)
        advanceUntilIdle()
        assertEquals(RelationshipState.NONE, vm.relationship.value.state)
        assertTrue(vm.relationship.value.error == null)
    }

    private fun profile(id: String) = AccountProfile(id, "user$id", "User $id", "Bio", null, null, null, "City", "https://example.com", "2026-08-01T00:00:00Z", "2026-08-01T00:00:00Z")
    private fun post(id: String) = SocialPost("post-$id", id, "hello", "public", created_at = "2026-08-01T00:00:00Z", updated_at = "2026-08-01T00:00:00Z", author = com.rasheed113.worksocial.domain.social.SocialPostAuthor("user$id", "User $id"))

    private class FakeAccountRepository(private val current: AccountProfile? = null, private val other: AccountProfile? = null, private val error: Throwable? = null, private val updateResult: AccountProfile? = null, private val updateError: Throwable? = null) : AccountRepository {
        val currentProfileCalls = mutableListOf<String>()
        val profileCalls = mutableListOf<String>()
        val updateInputs = mutableListOf<ProfileUpdateInput>()
        override suspend fun getProfile(profileId: String): AccountProfile? { profileCalls += profileId; error?.let { throw it }; return when (profileId) { current?.id -> current; other?.id -> other; else -> null } }
        override suspend fun getCurrentProfile(): AccountProfile? { currentProfileCalls += current?.id ?: ""; error?.let { throw it }; return current }
        override suspend fun updateCurrentProfile(input: ProfileUpdateInput): AccountProfile? { updateInputs += input; updateError?.let { throw it }; return updateResult ?: current?.copy(display_name = input.display_name) }
        override suspend fun uploadCurrentAvatar(jpegBytes: ByteArray): AccountProfile? = current
    }

    private class FakeFriendsRepository(private val data: FriendsData = FriendsData(emptyList(), emptyList(), emptyList())) : FriendsRepository {
        override suspend fun getFriends(): FriendsResult = FriendsResult.Success(data)
        override suspend fun sendRequest(receiverId: String): FriendMutationResult = FriendMutationResult.Success
        override suspend fun acceptRequest(requestId: String): FriendMutationResult = FriendMutationResult.Success
        override suspend fun rejectRequest(requestId: String): FriendMutationResult = FriendMutationResult.Success
        override suspend fun cancelRequest(requestId: String): FriendMutationResult = FriendMutationResult.Success
    }

    private class FakePostRepository(private val profilePosts: List<SocialPost> = emptyList(), private val error: Throwable? = null) : SocialPostRepository {
        val requestedProfileIds = mutableListOf<String>()
        override suspend fun getHomePosts() = emptyList<SocialPost>()
        override suspend fun getProfilePosts(profileId: String): List<SocialPost> { requestedProfileIds += profileId; error?.let { throw it }; return profilePosts.filter { it.profile_id == profileId } }
        override suspend fun createPost(content: String) = CreatePostResult.Failure("unused")
        override suspend fun likePost(postId: String) = LikeMutationResult.Failure("unused")
        override suspend fun unlikePost(postId: String) = LikeMutationResult.Failure("unused")
        override suspend fun getComments(postId: String) = CommentsResult.Failure("unused")
        override suspend fun createComment(postId: String, content: String) = CreateCommentResult.Failure("unused")
        override suspend fun deleteComment(commentId: String) = DeleteCommentResult.Failure("unused")
    }
}
