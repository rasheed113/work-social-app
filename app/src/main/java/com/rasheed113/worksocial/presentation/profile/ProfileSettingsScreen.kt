package com.rasheed113.worksocial.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import com.rasheed113.worksocial.presentation.ui.WorkSocialCard
import com.rasheed113.worksocial.presentation.ui.WorkSocialTypography

/** Native representation of the current Web Profile Settings page. */
@Composable
fun ProfileSettingsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Profile Settings", style = WorkSocialTypography.display)
        WorkSocialCard(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Privacy & Safety", style = WorkSocialTypography.title)
                Text("Manage who can interact with you.", style = WorkSocialTypography.body)
                Text("Blocked Users is present on the Web but the Android blocked-users screen is not implemented yet. It is intentionally not replaced with fake navigation.", style = WorkSocialTypography.body)
                Button(onClick = onBack) { Text("Back to Profile") }
            }
        }
    }
}
