package com.rasheed113.worksocial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rasheed113.worksocial.infrastructure.auth.SupabaseAuthRepository
import com.rasheed113.worksocial.presentation.auth.AuthViewModel
import com.rasheed113.worksocial.presentation.auth.AuthViewModelFactory
import com.rasheed113.worksocial.presentation.ui.WorkSocialApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val supabase = (application as WorkSocialApplication).supabase
        supabase.handleDeeplinks(intent)
        setContent {
            val repository = SupabaseAuthRepository(supabase.auth)
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(repository))
            WorkSocialApp(authViewModel)
        }
    }
}
