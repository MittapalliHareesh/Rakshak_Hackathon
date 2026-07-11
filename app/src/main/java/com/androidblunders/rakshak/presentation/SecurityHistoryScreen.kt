package com.androidblunders.rakshak.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.androidblunders.rakshak.ui.screens.DashboardViewModel
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val results by viewModel.recentResults.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security History", color = Color(0xFF002045)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            if (results.isEmpty()) {
                item {
                    Text(
                        "No messages or call transcript segments have been analyzed in this app session.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            items(results, key = { "${it.timestamp}:${it.sender}:${it.messageBody.hashCode()}" }) { result ->
                val risky = result.score.score >= 0.35f
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(result.sender, fontWeight = FontWeight.Bold)
                        Text(result.messageBody.take(180))
                        Text(
                            "${result.status} · ${(result.score.score * 100).toInt()}% · " +
                                DateFormat.getDateTimeInstance().format(Date(result.timestamp)),
                            color = if (risky) Color(0xFFBA1A1A) else Color(0xFF0A6C44),
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
