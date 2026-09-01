package com.rasheed113.worksocial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rasheed113.worksocial.infrastructure.account.SupabaseAccountRepository
import com.rasheed113.worksocial.infrastructure.activity.SupabaseActivityRepository
import com.rasheed113.worksocial.infrastructure.auth.SupabaseAuthRepository
import com.rasheed113.worksocial.infrastructure.chat.SupabaseChatRepository
import com.rasheed113.worksocial.infrastructure.friends.SupabaseFriendsRepository
import com.rasheed113.worksocial.infrastructure.social.SupabaseSocialPostRepository
import com.rasheed113.worksocial.presentation.auth.AuthViewModel
import com.rasheed113.worksocial.presentation.auth.AuthViewModelFactory
import com.rasheed113.worksocial.presentation.ui.WorkSocialApp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val supabase = (application as WorkSocialApplication).supabase
        supabase.handleDeeplinks(intent)
        val authRepository = SupabaseAuthRepository(supabase.auth)
        val accountRepository = SupabaseAccountRepository(supabase.postgrest, supabase.auth, supabase.storage)
        val socialPostRepository = SupabaseSocialPostRepository(supabase.postgrest, supabase.auth, supabase.storage)
        val activityRepository = SupabaseActivityRepository(supabase.postgrest, supabase.auth, supabase.realtime)
        val friendsRepository = SupabaseFriendsRepository(supabase.postgrest, supabase.auth)
        val chatRepository = SupabaseChatRepository(supabase.postgrest, supabase.auth, supabase.realtime)
        setContent {
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepository))
            WorkSocialApp(authViewModel, accountRepository, socialPostRepository, activityRepository, friendsRepository, chatRepository)
        }
    }
}
