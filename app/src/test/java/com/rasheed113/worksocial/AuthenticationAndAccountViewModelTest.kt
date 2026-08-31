package com.rasheed113.worksocial

import com.rasheed113.worksocial.domain.account.AccountProfile
import com.rasheed113.worksocial.domain.account.AccountRepository
import com.rasheed113.worksocial.domain.account.AccountState
import com.rasheed113.worksocial.domain.auth.AuthRepository
import com.rasheed113.worksocial.domain.auth.AuthState
import com.rasheed113.worksocial.domain.auth.AuthenticatedIdentity
import com.rasheed113.worksocial.domain.auth.SignUpOutcome
import com.rasheed113.worksocial.presentation.account.AccountViewModel
import com.rasheed113.worksocial.presentation.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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

class AuthenticationAndAccountViewModelTest {
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
    fun authViewModelRestoresAuthenticatedSession() = runTest {
        val repository = FakeAuthRepository(AuthState.SignedIn(AuthenticatedIdentity("user-1", "user@example.com")))
        val viewModel = AuthViewModel(repository)

        advanceUntilIdle()

        assertEquals(repository.authState.value, viewModel.uiState.value.auth)
    }

    @Test
    fun authViewModelLogoutTransitionsToSignedOut() = runTest {
        val repository = FakeAuthRepository(AuthState.SignedIn(AuthenticatedIdentity("user-1", "user@example.com")))
        val viewModel = AuthViewModel(repository)
        advanceUntilIdle()

        viewModel.signOut()
        advanceUntilIdle()

        assertEquals(AuthState.SignedOut, viewModel.uiState.value.auth)
        assertTrue(repository.signOutCalled)
    }

    @Test
    fun accountViewModelLoadsRealProfileContract() = runTest {
        val profile = AccountProfile(
            id = "user-1",
            username = "rasheed",
            display_name = "Rasheed",
            bio = "Developer",
            avatar_url = null,
            location = "Karachi",
        )
        val viewModel = AccountViewModel(FakeAccountRepository(profile = profile))

        viewModel.load("user-1")
        advanceUntilIdle()

        assertEquals(AccountState.Success(profile), viewModel.state.value)
    }

    @Test
    fun accountViewModelPreservesEmptyProfileState() = runTest {
        val viewModel = AccountViewModel(FakeAccountRepository(profile = null))

        viewModel.load("user-1")
        advanceUntilIdle()

        assertEquals(AccountState.Empty, viewModel.state.value)
    }

    @Test
    fun accountViewModelPreservesBackendErrors() = runTest {
        val viewModel = AccountViewModel(FakeAccountRepository(error = IllegalStateException("network failure")))

        viewModel.load("user-1")
        advanceUntilIdle()

        assertEquals(AccountState.Error("network failure"), viewModel.state.value)
    }

    private class FakeAuthRepository(initial: AuthState) : AuthRepository {
        override val authState = MutableStateFlow(initial)
        var signOutCalled = false

        override suspend fun signIn(email: String, password: String) = Unit

        override suspend fun signUp(email: String, password: String, displayName: String) = SignUpOutcome(false)

        override suspend fun signOut() {
            signOutCalled = true
            authState.value = AuthState.SignedOut
        }
    }

    private class FakeAccountRepository(
        private val profile: AccountProfile? = null,
        private val error: Throwable? = null,
    ) : AccountRepository {
        override suspend fun getProfile(profileId: String): AccountProfile? {
            error?.let { throw it }
            return profile
        }
    }
}
