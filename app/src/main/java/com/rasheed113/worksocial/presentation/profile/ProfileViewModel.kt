package com.rasheed113.worksocial.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasheed113.worksocial.domain.account.AccountProfile
import com.rasheed113.worksocial.domain.account.AccountRepository
import com.rasheed113.worksocial.domain.account.ProfileBlockStatus
import com.rasheed113.worksocial.domain.account.ProfileUpdateInput
import com.rasheed113.worksocial.domain.account.validateProfileUpdate
import com.rasheed113.worksocial.domain.friends.FriendMutationResult
import com.rasheed113.worksocial.domain.friends.FriendsRepository
import com.rasheed113.worksocial.domain.friends.FriendsResult
import com.rasheed113.worksocial.domain.friends.RelationshipState
import com.rasheed113.worksocial.domain.social.SocialPost
import com.rasheed113.worksocial.domain.social.SocialPostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProfileLoadState { data object Loading : ProfileLoadState; data class Success(val profile: AccountProfile) : ProfileLoadState; data object NotFound : ProfileLoadState; data object BlockedByMe : ProfileLoadState; data class Error(val message: String) : ProfileLoadState }
data class ProfileEditState(val displayName: String = "", val bio: String = "", val dateOfBirth: String = "", val gender: String = "", val location: String = "", val website: String = "", val saving: Boolean = false, val error: String? = null, val saved: Boolean = false)
data class ProfileRelationshipState(val state: RelationshipState = RelationshipState.NONE, val requestId: String? = null, val loading: Boolean = false, val error: String? = null)
data class ProfilePostsState(val loading: Boolean = true, val posts: List<SocialPost> = emptyList(), val error: String? = null)
data class ProfileSocialState(val following: Boolean = false, val blockStatus: ProfileBlockStatus = ProfileBlockStatus(), val busy: Boolean = false, val error: String? = null)

class ProfileViewModel(private val accountRepository: AccountRepository, private val friendsRepository: FriendsRepository, private val socialPostRepository: SocialPostRepository, private val currentUserId: String, private val targetProfileId: String?) : ViewModel() {
    val isOwnProfile = targetProfileId == null || targetProfileId == currentUserId
    private val effectiveProfileId get() = targetProfileId ?: currentUserId
    private val _profile = MutableStateFlow<ProfileLoadState>(ProfileLoadState.Loading); val profile: StateFlow<ProfileLoadState> = _profile.asStateFlow()
    private val _edit = MutableStateFlow(ProfileEditState()); val edit: StateFlow<ProfileEditState> = _edit.asStateFlow()
    private val _relationship = MutableStateFlow(ProfileRelationshipState()); val relationship: StateFlow<ProfileRelationshipState> = _relationship.asStateFlow()
    private val _social = MutableStateFlow(ProfileSocialState()); val social: StateFlow<ProfileSocialState> = _social.asStateFlow()
    private val _posts = MutableStateFlow(ProfilePostsState()); val posts: StateFlow<ProfilePostsState> = _posts.asStateFlow()
    init { load() }

    fun load() {
        viewModelScope.launch {
            _profile.value = ProfileLoadState.Loading; _posts.value = ProfilePostsState(loading = true); _relationship.value = ProfileRelationshipState(loading = !isOwnProfile); _social.value = ProfileSocialState()
            try {
                if (!isOwnProfile) {
                    val social = accountRepository.getProfileRelationship(effectiveProfileId)
                    _social.value = ProfileSocialState(social.following, social.blockStatus)
                    if (social.blockStatus.blocked_by_me) { _profile.value = ProfileLoadState.BlockedByMe; _posts.value = ProfilePostsState(false); _relationship.value = ProfileRelationshipState(); return@launch }
                    if (social.blockStatus.blocked_me) { _profile.value = ProfileLoadState.NotFound; _posts.value = ProfilePostsState(false); _relationship.value = ProfileRelationshipState(); return@launch }
                }
                val profile = if (isOwnProfile) accountRepository.getCurrentProfile() else accountRepository.getProfile(effectiveProfileId)
                if (profile == null) { _profile.value = ProfileLoadState.NotFound; _posts.value = ProfilePostsState(false); return@launch }
                _profile.value = ProfileLoadState.Success(profile); syncEditFromProfile(profile)
                if (!isOwnProfile) loadRelationship()
                loadPosts()
            } catch (error: Throwable) {
                _profile.value = ProfileLoadState.Error(error.message?.takeIf(String::isNotBlank) ?: "Unable to load this profile right now."); _posts.value = ProfilePostsState(false); _relationship.value = ProfileRelationshipState(error = error.message); _social.value = _social.value.copy(error = error.message)
            }
        }
    }
    fun updateEdit(field: ProfileEditField, value: String) { _edit.value = when (field) { ProfileEditField.DisplayName -> _edit.value.copy(displayName=value,error=null,saved=false); ProfileEditField.Bio -> _edit.value.copy(bio=value,error=null,saved=false); ProfileEditField.DateOfBirth -> _edit.value.copy(dateOfBirth=value,error=null,saved=false); ProfileEditField.Gender -> _edit.value.copy(gender=value,error=null,saved=false); ProfileEditField.Location -> _edit.value.copy(location=value,error=null,saved=false); ProfileEditField.Website -> _edit.value.copy(website=value,error=null,saved=false) } }
    fun saveProfile() {
        if (!isOwnProfile) return
        val input = _edit.value.toInput(); validateProfileUpdate(input)?.let { _edit.value = _edit.value.copy(error=it,saving=false,saved=false); return }
        viewModelScope.launch { _edit.value = _edit.value.copy(saving=true,error=null,saved=false); runCatching { accountRepository.updateCurrentProfile(input) }.onSuccess { profile -> if (profile == null) _edit.value = _edit.value.copy(saving=false,error="Profile could not be saved.") else { _profile.value=ProfileLoadState.Success(profile); syncEditFromProfile(profile); _edit.value=_edit.value.copy(saving=false,saved=true,error=null) } }.onFailure { _edit.value=_edit.value.copy(saving=false,error=it.message ?: "Profile could not be saved.",saved=false) } }
    }
    fun uploadAvatar(jpegBytes: ByteArray) {
        if (!isOwnProfile) return
        viewModelScope.launch { _edit.value=_edit.value.copy(saving=true,error=null,saved=false); runCatching { accountRepository.uploadCurrentAvatar(jpegBytes) }.onSuccess { profile -> if (profile==null) _edit.value=_edit.value.copy(saving=false,error="Profile photo could not be saved.") else { _profile.value=ProfileLoadState.Success(profile); syncEditFromProfile(profile); _edit.value=_edit.value.copy(saving=false,saved=true,error=null) } }.onFailure { _edit.value=_edit.value.copy(saving=false,error=it.message ?: "Profile photo could not be saved.") } }
    }
    fun toggleFollow() {
        if (isOwnProfile || _social.value.busy) return
        viewModelScope.launch { val next=!_social.value.following; _social.value=_social.value.copy(busy=true,error=null); runCatching { accountRepository.setFollowing(effectiveProfileId,next) }.onSuccess { _social.value=_social.value.copy(following=next,busy=false) }.onFailure { _social.value=_social.value.copy(busy=false,error=it.message ?: "Unable to update follow status.") } }
    }
    fun blockProfile() {
        if (isOwnProfile || _social.value.busy) return
        viewModelScope.launch { _social.value=_social.value.copy(busy=true,error=null); runCatching { accountRepository.blockProfile(effectiveProfileId) }.onSuccess { _social.value=_social.value.copy(busy=false,blockStatus=ProfileBlockStatus(blocked_by_me=true,blocked=true)); _profile.value=ProfileLoadState.BlockedByMe }.onFailure { _social.value=_social.value.copy(busy=false,error=it.message ?: "Unable to block this user.") } }
    }
    fun unblockProfile() {
        if (isOwnProfile || _social.value.busy) return
        viewModelScope.launch { _social.value=_social.value.copy(busy=true,error=null); runCatching { accountRepository.unblockProfile(effectiveProfileId) }.onSuccess { _social.value=_social.value.copy(busy=false); load() }.onFailure { _social.value=_social.value.copy(busy=false,error=it.message ?: "Unable to unblock this user.") } }
    }
    fun sendFriendRequest() { if (isOwnProfile || _relationship.value.loading) return; viewModelScope.launch { _relationship.value=_relationship.value.copy(loading=true,error=null); when(val r=friendsRepository.sendRequest(effectiveProfileId)){ FriendMutationResult.Success -> _relationship.value=ProfileRelationshipState(RelationshipState.OUTGOING_PENDING); is FriendMutationResult.Failure -> _relationship.value=_relationship.value.copy(loading=false,error=r.message) } } }
    fun acceptFriendRequest() { val id=_relationship.value.requestId ?: return; viewModelScope.launch { _relationship.value=_relationship.value.copy(loading=true,error=null); when(val r=friendsRepository.acceptRequest(id)){ FriendMutationResult.Success -> _relationship.value=ProfileRelationshipState(RelationshipState.FRIENDS); is FriendMutationResult.Failure -> _relationship.value=_relationship.value.copy(loading=false,error=r.message) } } }
    fun cancelFriendRequest() { val id=_relationship.value.requestId ?: return; viewModelScope.launch { _relationship.value=_relationship.value.copy(loading=true,error=null); when(val r=friendsRepository.cancelRequest(id)){ FriendMutationResult.Success -> _relationship.value=ProfileRelationshipState(RelationshipState.NONE); is FriendMutationResult.Failure -> _relationship.value=_relationship.value.copy(loading=false,error=r.message) } } }
    private suspend fun loadRelationship() { when(val result=friendsRepository.getFriends()){ is FriendsResult.Success -> { val person=result.data.people.firstOrNull{it.profile.id==effectiveProfileId}; val incoming=result.data.incomingRequests.firstOrNull{it.profile.id==effectiveProfileId}; val outgoing=result.data.outgoingRequests.firstOrNull{it.profile.id==effectiveProfileId}; _relationship.value=ProfileRelationshipState(person?.relationship ?: when { incoming!=null->RelationshipState.INCOMING_PENDING; outgoing!=null->RelationshipState.OUTGOING_PENDING; else->RelationshipState.NONE },incoming?.id ?: outgoing?.id) }; is FriendsResult.Failure -> _relationship.value=ProfileRelationshipState(error=result.message) } }
    private suspend fun loadPosts() { runCatching { socialPostRepository.getProfilePosts(effectiveProfileId) }.onSuccess{_posts.value=ProfilePostsState(false,it)}.onFailure{_posts.value=ProfilePostsState(false,error=it.message ?: "Unable to load profile posts.")} }
    private fun syncEditFromProfile(profile: AccountProfile) { _edit.value=ProfileEditState(profile.display_name,profile.bio.orEmpty(),profile.date_of_birth.orEmpty(),profile.gender.orEmpty(),profile.location.orEmpty(),profile.website.orEmpty()) }
    private fun ProfileEditState.toInput()=ProfileUpdateInput(displayName,bio,dateOfBirth,gender,location,website)
}
enum class ProfileEditField { DisplayName, Bio, DateOfBirth, Gender, Location, Website }
