package com.rasheed113.worksocial.presentation.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rasheed113.worksocial.domain.account.AccountRepository
import com.rasheed113.worksocial.domain.account.AccountState
import com.rasheed113.worksocial.domain.activity.ActivityRepository
import com.rasheed113.worksocial.domain.activity.ActivityState
import com.rasheed113.worksocial.domain.auth.AuthState
import com.rasheed113.worksocial.domain.friends.FriendsRepository
import com.rasheed113.worksocial.domain.social.SocialPostRepository
import com.rasheed113.worksocial.presentation.account.AccountViewModel
import com.rasheed113.worksocial.presentation.account.AccountViewModelFactory
import com.rasheed113.worksocial.presentation.activity.ActivityScreen
import com.rasheed113.worksocial.presentation.activity.ActivityViewModel
import com.rasheed113.worksocial.presentation.activity.ActivityViewModelFactory
import com.rasheed113.worksocial.presentation.auth.AuthUiState
import com.rasheed113.worksocial.presentation.auth.AuthViewModel
import com.rasheed113.worksocial.presentation.friends.FriendsScreen
import com.rasheed113.worksocial.presentation.friends.FriendsViewModel
import com.rasheed113.worksocial.presentation.friends.FriendsViewModelFactory
import com.rasheed113.worksocial.presentation.navigation.AppDestination
import com.rasheed113.worksocial.presentation.profile.ProfileScreen
import com.rasheed113.worksocial.presentation.social.CreatePostScreen
import com.rasheed113.worksocial.presentation.social.SocialHomeScreen

data class SocialNotificationTarget(val postId: String, val commentId: String?)

@Composable
fun WorkSocialApp(viewModel: AuthViewModel, accountRepository: AccountRepository, socialPostRepository: SocialPostRepository, activityRepository: ActivityRepository, friendsRepository: FriendsRepository) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WorkSocialTheme {
        when (val auth = state.auth) {
            AuthState.Initializing -> LoadingScreen()
            AuthState.SignedOut -> AuthScreen(state, viewModel)
            is AuthState.Error -> AuthScreen(state, viewModel)
            is AuthState.SignedIn -> AuthenticatedShell(auth.identity.userId, viewModel, accountRepository, socialPostRepository, activityRepository, friendsRepository)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), verticalArrangement = Arrangement.Center, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Text("Restoring Work Social session", modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun AuthScreen(state: AuthUiState, viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var signUp by remember { mutableStateOf(false) }
    Scaffold(topBar = { AppTopBar(if (signUp) "Create Work Social account" else "Sign in to Work Social") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (signUp) "Create your real Work Social account" else "Use your Work Social account", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            if (signUp) OutlinedTextField(value = displayName, onValueChange = { displayName = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Display name") }, enabled = !state.busy)
            OutlinedTextField(value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Email") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email), enabled = !state.busy)
            OutlinedTextField(value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Password") }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password), enabled = !state.busy)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Button(modifier = Modifier.fillMaxWidth(), enabled = !state.busy && email.isNotBlank() && password.isNotBlank() && (!signUp || displayName.isNotBlank()), onClick = { if (signUp) viewModel.signUp(email, password, displayName) else viewModel.signIn(email, password) }) {
                if (state.busy) CircularProgressIndicator(strokeWidth = 2.dp) else Text(if (signUp) "Create account" else "Sign in")
            }
            TextButton(enabled = !state.busy, onClick = { signUp = !signUp }) { Text(if (signUp) "Already have an account" else "Create an account") }
        }
    }
}

@Composable
private fun AuthenticatedShell(userId: String, viewModel: AuthViewModel, accountRepository: AccountRepository, socialPostRepository: SocialPostRepository, activityRepository: ActivityRepository, friendsRepository: FriendsRepository) {
    val navController = rememberNavController()
    var socialRefreshToken by remember { mutableIntStateOf(0) }
    var socialNotificationTarget by remember { mutableStateOf<SocialNotificationTarget?>(null) }
    val accountViewModel: AccountViewModel = viewModel(key = "account-$userId", factory = AccountViewModelFactory(accountRepository))
    val accountState by accountViewModel.state.collectAsStateWithLifecycle()
    val activityViewModel: ActivityViewModel = viewModel(key = "activity-$userId", factory = ActivityViewModelFactory(activityRepository))
    val activityState by activityViewModel.state.collectAsStateWithLifecycle()
    val friendsViewModel: FriendsViewModel = viewModel(key = "friends-$userId", factory = FriendsViewModelFactory(friendsRepository))
    LaunchedEffect(userId) { accountViewModel.load(userId) }
    val destinations = listOf(AppDestination.Social, AppDestination.Friends, AppDestination.Activity, AppDestination.Profile, AppDestination.WorkHouse)
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val isCreatePost = currentRoute == AppDestination.CreatePost.route
    val unreadCount = (activityState as? ActivityState.Success)?.unreadCount ?: 0
    val topTitle = when (currentRoute) {
        AppDestination.Friends.route -> "Friends"
        AppDestination.Profile.route, AppDestination.PublicProfile.route -> "Profile"
        AppDestination.WorkHouse.route -> "Work House"
        else -> "Work Social"
    }
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = { if (!isCreatePost) AppTopBar(topTitle) }, bottomBar = {
        if (!isCreatePost) NavigationBar {
            destinations.forEach { destination ->
                NavigationBarItem(selected = currentRoute == destination.route, onClick = {
                    navController.navigate(destination.route) { launchSingleTop = true }
                }, icon = {
                    when (destination) {
                        AppDestination.Activity -> if (unreadCount > 0) BadgedBox(badge = { Badge { Text(if (unreadCount > 99) "99+" else unreadCount.toString()) } }) { Text("♢") } else Text("♢")
                        AppDestination.Social -> Text("⌂")
                        AppDestination.Friends -> Text("♧")
                        AppDestination.Profile, AppDestination.PublicProfile -> Text("◉")
                        AppDestination.WorkHouse -> Text("▣")
                        AppDestination.CreatePost -> Text("＋")
                    }
                }, label = { Text(destination.label) })
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = navController, startDestination = AppDestination.Social.route, modifier = Modifier.weight(1f)) {
                composable(AppDestination.Social.route) {
                    val target = socialNotificationTarget
                    SocialHomeScreen(repository = socialPostRepository, refreshToken = socialRefreshToken, targetPostId = target?.postId, targetCommentId = target?.commentId, onTargetConsumed = { socialNotificationTarget = null }, onCreatePost = { navController.navigate(AppDestination.CreatePost.route) })
                }
                composable(AppDestination.Friends.route) { FriendsScreen(friendsViewModel, onOpenProfile = { profileId -> navController.navigate("profile/${Uri.encode(profileId)}") }) }
                composable(AppDestination.Activity.route) { ActivityScreen(viewModel = activityViewModel, onOpenPost = { postId, commentId -> socialNotificationTarget = SocialNotificationTarget(postId, commentId); navController.navigate(AppDestination.Social.route) { launchSingleTop = true } }) }
                composable(AppDestination.Profile.route) { ProfileScreen(accountRepository, friendsRepository, socialPostRepository, userId, null) }
                composable(AppDestination.PublicProfile.route, arguments = listOf(navArgument("profileId") { type = NavType.StringType })) { entry ->
                    ProfileScreen(accountRepository, friendsRepository, socialPostRepository, userId, entry.arguments?.getString("profileId"))
                }
                composable(AppDestination.CreatePost.route) { CreatePostScreen(repository = socialPostRepository, onCreated = { socialRefreshToken += 1 }, onBack = { navController.popBackStack() }) }
                composable(AppDestination.WorkHouse.route) { AuthenticatedSection("Work House", accountState) { accountViewModel.retry(userId) } }
            }
            if (!isCreatePost) {
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) { TextButton(onClick = viewModel::signOut) { Text("Sign out") } }
            }
        }
    }
}

@Composable
private fun AuthenticatedSection(title: String, accountState: AccountState, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        when (accountState) {
            AccountState.Loading -> { CircularProgressIndicator(); Text("Loading your Work Social account…") }
            is AccountState.Success -> { Text(accountState.profile.display_name, style = MaterialTheme.typography.titleLarge); Text("@${accountState.profile.username}"); Text("User ID: ${accountState.profile.id}"); accountState.profile.location?.takeIf { it.isNotBlank() }?.let { Text("Location: $it") }; accountState.profile.bio?.takeIf { it.isNotBlank() }?.let { Text(it) }; Text("Real account data loaded from the Work Social profiles table.", color = MaterialTheme.colorScheme.primary) }
            AccountState.Empty -> { Text("Your authenticated account profile was not found."); Text("No placeholder account data is shown."); TextButton(onClick = onRetry) { Text("Retry") } }
            is AccountState.Error -> { Text(accountState.message, color = MaterialTheme.colorScheme.error); Text("The account request failed; no fallback data is displayed."); TextButton(onClick = onRetry) { Text("Retry") } }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(title: String) { TopAppBar(title = { Text(title) }) }
