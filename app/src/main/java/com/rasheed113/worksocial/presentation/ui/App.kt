package com.rasheed113.worksocial.presentation.ui

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rasheed113.worksocial.domain.account.AccountState
import com.rasheed113.worksocial.domain.auth.AuthState
import com.rasheed113.worksocial.domain.account.AccountRepository
import com.rasheed113.worksocial.presentation.account.AccountViewModel
import com.rasheed113.worksocial.presentation.account.AccountViewModelFactory
import com.rasheed113.worksocial.presentation.auth.AuthUiState
import com.rasheed113.worksocial.presentation.auth.AuthViewModel
import com.rasheed113.worksocial.presentation.navigation.AppDestination

@Composable
fun WorkSocialApp(viewModel: AuthViewModel, accountRepository: AccountRepository) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WorkSocialTheme {
        when (val auth = state.auth) {
            AuthState.Initializing -> LoadingScreen()
            AuthState.SignedOut -> AuthScreen(state, viewModel)
            is AuthState.Error -> AuthScreen(state, viewModel)
            is AuthState.SignedIn -> AuthenticatedShell(auth.identity.userId, viewModel, accountRepository)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (signUp) "Create your real Work Social account" else "Use your Work Social account",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp),
            )
            if (signUp) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Display name") },
                    enabled = !state.busy,
                )
            }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Email") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = !state.busy,
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !state.busy,
            )
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            state.notice?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.busy && email.isNotBlank() && password.isNotBlank() && (!signUp || displayName.isNotBlank()),
                onClick = {
                    if (signUp) viewModel.signUp(email, password, displayName)
                    else viewModel.signIn(email, password)
                },
            ) {
                if (state.busy) CircularProgressIndicator(strokeWidth = 2.dp)
                else Text(if (signUp) "Create account" else "Sign in")
            }
            TextButton(enabled = !state.busy, onClick = { signUp = !signUp }) {
                Text(if (signUp) "Already have an account" else "Create an account")
            }
        }
    }
}

@Composable
private fun AuthenticatedShell(
    userId: String,
    viewModel: AuthViewModel,
    accountRepository: AccountRepository,
) {
    val navController = rememberNavController()
    val accountViewModel: AccountViewModel = viewModel(
        key = "account-$userId",
        factory = AccountViewModelFactory(accountRepository),
    )
    val accountState by accountViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(userId) { accountViewModel.load(userId) }

    val destinations = listOf(AppDestination.Social, AppDestination.WorkHouse)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { AppTopBar("Work Social") },
        bottomBar = {
            NavigationBar {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = { navController.navigate(destination.route) { launchSingleTop = true } },
                        icon = {},
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = AppDestination.Social.route,
                modifier = Modifier.weight(1f),
            ) {
                composable(AppDestination.Social.route) {
                    AuthenticatedSection(
                        title = "Social",
                        accountState = accountState,
                        onRetry = { accountViewModel.retry(userId) },
                    )
                }
                composable(AppDestination.WorkHouse.route) {
                    AuthenticatedSection(
                        title = "Work House",
                        accountState = accountState,
                        onRetry = { accountViewModel.retry(userId) },
                    )
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = viewModel::signOut) { Text("Sign out") }
            }
        }
    }
}

@Composable
private fun AuthenticatedSection(
    title: String,
    accountState: AccountState,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        when (accountState) {
            AccountState.Loading -> {
                CircularProgressIndicator()
                Text("Loading your Work Social account…")
            }
            is AccountState.Success -> {
                Text(accountState.profile.display_name, style = MaterialTheme.typography.titleLarge)
                Text("@${accountState.profile.username}")
                Text("User ID: ${accountState.profile.id}")
                accountState.profile.location?.takeIf { it.isNotBlank() }?.let { Text("Location: $it") }
                accountState.profile.bio?.takeIf { it.isNotBlank() }?.let { Text(it) }
                Text(
                    "Real account data loaded from the Work Social profiles table.",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            AccountState.Empty -> {
                Text("Your authenticated account profile was not found.")
                Text("No placeholder account data is shown.")
                TextButton(onClick = onRetry) { Text("Retry") }
            }
            is AccountState.Error -> {
                Text(accountState.message, color = MaterialTheme.colorScheme.error)
                Text("The account request failed; no fallback data is displayed.")
                TextButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
private fun AppTopBar(title: String) {
    TopAppBar(title = { Text(title) })
}
