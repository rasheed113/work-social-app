package com.rasheed113.worksocial

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rasheed113.worksocial.infrastructure.account.SupabaseAccountRepository
import com.rasheed113.worksocial.infrastructure.activity.SupabaseActivityRepository
import com.rasheed113.worksocial.infrastructure.auth.SupabaseAuthRepository
import com.rasheed113.worksocial.infrastructure.calls.SupabaseCallRepository
import com.rasheed113.worksocial.infrastructure.chat.SupabaseChatRepository
import com.rasheed113.worksocial.infrastructure.friends.SupabaseFriendsRepository
import com.rasheed113.worksocial.infrastructure.notifications.FcmRegistrationManager
import com.rasheed113.worksocial.infrastructure.notifications.WorkSocialFirebaseMessagingService
import com.rasheed113.worksocial.infrastructure.social.SupabaseSocialPostRepository
import com.rasheed113.worksocial.infrastructure.work.SupabaseWorkHouseRepository
import com.rasheed113.worksocial.platform.calls.PendingIncomingCallStore
import com.rasheed113.worksocial.platform.calls.WebRtcCallEngine
import com.rasheed113.worksocial.presentation.auth.AuthViewModel
import com.rasheed113.worksocial.presentation.auth.AuthViewModelFactory
import com.rasheed113.worksocial.presentation.ui.WorkSocialApp
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PendingIncomingCallStore.accept(intent.getStringExtra(EXTRA_CALL_ID))
        ensureIncomingCallChannel()
        requestNotificationPermissionIfNeeded()

        val supabase = (application as WorkSocialApplication).supabase
        lifecycleScope.launch { supabase.handleDeeplinks(intent) }

        val authRepository = SupabaseAuthRepository(supabase.auth, supabase.postgrest)
        val accountRepository = SupabaseAccountRepository(supabase.postgrest, supabase.auth, supabase.storage)
        val socialPostRepository = SupabaseSocialPostRepository(supabase.postgrest, supabase.auth, supabase.storage)
        val activityRepository = SupabaseActivityRepository(supabase.postgrest, supabase.auth, supabase.realtime)
        val friendsRepository = SupabaseFriendsRepository(supabase.postgrest, supabase.auth)
        val chatRepository = SupabaseChatRepository(supabase.postgrest, supabase.auth, supabase.realtime)
        val callRepository = SupabaseCallRepository(supabase.postgrest, supabase.auth, supabase.realtime)
        val workHouseRepository = SupabaseWorkHouseRepository(supabase.postgrest, supabase.auth)
        val callEngine = WebRtcCallEngine(applicationContext)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                supabase.auth.sessionStatus.collect { status: SessionStatus ->
                    if (status is SessionStatus.Authenticated) {
                        FcmRegistrationManager.sync(supabase.postgrest, supabase.auth)
                    }
                }
            }
        }

        setContent {
            val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(authRepository))
            WorkSocialApp(authViewModel, accountRepository, socialPostRepository, activityRepository, friendsRepository, chatRepository, callRepository, workHouseRepository, callEngine)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PendingIncomingCallStore.accept(intent.getStringExtra(EXTRA_CALL_ID))
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun ensureIncomingCallChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(WorkSocialFirebaseMessagingService.CALL_CHANNEL_ID, "Incoming calls", NotificationManager.IMPORTANCE_HIGH).apply { description = "Real Work Social incoming call alerts" })
    }

    companion object {
        const val ACTION_INCOMING_CALL = "com.rasheed113.worksocial.action.INCOMING_CALL"
        const val EXTRA_CALL_ID = "work_social.call_id"
        const val EXTRA_CONVERSATION_ID = "work_social.conversation_id"
        const val EXTRA_CALLER_ID = "work_social.caller_id"
        const val EXTRA_CALL_KIND = "work_social.call_kind"
    }
}
