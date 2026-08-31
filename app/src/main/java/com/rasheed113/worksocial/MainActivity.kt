package com.rasheed113.worksocial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rasheed113.worksocial.infrastructure.account.SupabaseAccountRepository
import com.rasheed113.worksocial.infrastructure.auth.SupabaseAuthRepository
import com.rasheed113.worksocial.presentation.auth.AuthViewModel
import com.rasheed113.worksocial.presentation.auth.AuthViewModelFactory
import com.rasheed113.worksocial.presentation.ui.WorkSocialApp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val supabase = (application as WorkSocialApplication).supabase
        supabase.handleDeeplinks(intent)
        val authRepository = SupabaseAuthRepository(supabase.auth)
        val accountRepository = SupabaseAccountRepository(supabase.postgrest)
        setContent {
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepository))
            WorkSocialApp(authViewModel, accountRepository)
        }
    }
}
