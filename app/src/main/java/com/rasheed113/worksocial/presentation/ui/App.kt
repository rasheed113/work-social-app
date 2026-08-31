package com.rasheed113.worksocial.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rasheed113.worksocial.domain.auth.AuthState
import com.rasheed113.worksocial.presentation.auth.AuthUiState
import com.rasheed113.worksocial.presentation.auth.AuthViewModel
import com.rasheed113.worksocial.presentation.navigation.AppDestination

@Composable
fun WorkSocialApp(viewModel: AuthViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WorkSocialTheme {
        when (val auth = state.auth) {
            AuthState.Initializing -> LoadingScreen()
            AuthState.SignedOut -> AuthScreen(state, viewModel)
            is AuthState.Error -> AuthScreen(state, viewModel)
            is AuthState.SignedIn -> AuthenticatedShell(auth.identity.userId, viewModel)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.padding(24.dp))
        Text("Restoring Work Social session", modifier = Modifier.padding(horizontal = 24.dp))
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (signUp) TextField(displayName, { displayName = it }, label = { Text("Display name") })
            TextField(email, { email = it }, label = { Text("Email") })
            TextField(password, { password = it }, label = { Text("Password") })
            state.error?.let { Text(it) }
            Button(
                enabled = !state.busy && email.isNotBlank() && password.isNotBlank() && (!signUp || displayName.isNotBlank()),
                onClick = {
                    if (signUp) viewModel.signUp(email, password, displayName)
                    else viewModel.signIn(email, password)
                }
            ) { Text(if (signUp) "Create account" else "Sign in") }
            Button(enabled = !state.busy, onClick = { signUp = !signUp }) {
                Text(if (signUp) "Already have an account" else "Create an account")
            }
        }
    }
}

@Composable
private fun AuthenticatedShell(userId: String, viewModel: AuthViewModel) {
    val navController = rememberNavController()
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
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = AppDestination.Social.route,
                modifier = Modifier.weight(1f)
            ) {
                composable(AppDestination.Social.route) {
                    FoundationSection(
                        title = "Social",
                        userId = userId,
                        description = "The native Android shell is authenticated against the real Work Social account. Social feature slices will be added from the audited website contracts; no mock records are shown."
                    )
                }
                composable(AppDestination.WorkHouse.route) {
                    FoundationSection(
                        title = "Work House",
                        userId = userId,
                        description = "Work House is reserved for the real worker contracts already present in the website. No fake totals, finance values, diary entries, or work records are shown."
                    )
                }
            }
            Button(onClick = viewModel::signOut, modifier = Modifier.padding(16.dp)) { Text("Sign out") }
        }
    }
}

@Composable
private fun FoundationSection(title: String, userId: String, description: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title)
        Text(description)
        Text("Authenticated user: $userId")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(title: String) {
    TopAppBar(title = { Text(title) })
}
