package com.androidblunders.rakshak.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color(0xFF002045)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Protection mode", style = MaterialTheme.typography.titleLarge)
            Text(
                "Call and SMS protection stay enabled in this hackathon build. " +
                    "Permission readiness, VOICE socket status, and local/cloud AI status " +
                    "are shown on the dashboard.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text("Automatic call activation", style = MaterialTheme.typography.titleMedium)
            Text("Starts after Android reports the call as connected (OFFHOOK).")
            Text("Message context", style = MaterialTheme.typography.titleMedium)
            Text("Each live transcript analysis includes up to the latest 25 captured messages.")
        }
    }
}
