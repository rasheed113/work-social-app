package com.rasheed113.worksocial.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthStateTest {
    @Test
    fun signedInIdentity_preservesRealUserIdAndEmail() {
        val state = AuthState.SignedIn(AuthenticatedIdentity("real-user-id", "user@example.com"))
        assertEquals("real-user-id", state.identity.userId)
        assertEquals("user@example.com", state.identity.email)
    }
}
