package com.shubham.ondevicerag.feature.chat.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shubham.ondevicerag.feature.chat.domain.model.ChatMessage
import com.shubham.ondevicerag.feature.chat.domain.model.ChatRole
import com.shubham.ondevicerag.feature.chat.domain.model.ModelDownloadStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStopGeneration: () -> Unit,
    onDownloadModel: () -> Unit,
    onNewChat: () -> Unit
) {
    val listState = rememberLazyListState()
    val latestMessageContent = uiState.messages.lastOrNull()?.content

    LaunchedEffect(uiState.messages.size, latestMessageContent) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Gemma Chat",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = runtimeStatus(uiState),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNewChat, enabled = uiState.messages.isNotEmpty()) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "New chat")
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                value = uiState.input,
                isGenerating = uiState.isGenerating,
                enabled = uiState.modelStatus.isModelAvailable && uiState.isEngineReady,
                onInputChange = onInputChange,
                onSend = onSend,
                onStopGeneration = onStopGeneration
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ModelStatusPanel(
                status = uiState.modelStatus,
                isEngineReady = uiState.isEngineReady,
                error = uiState.error,
                onDownloadModel = onDownloadModel
            )

            if (uiState.messages.isEmpty()) {
                EmptyChatState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        MessageBubble(message = message)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelStatusPanel(
    status: ModelDownloadStatus,
    isEngineReady: Boolean,
    error: String?,
    onDownloadModel: () -> Unit
) {
    if (status.isModelAvailable && isEngineReady && error == null) return

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (status.isDownloading) {
                Text(
                    text = "Downloading Gemma 4 E2B ${downloadPercent(status)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                LinearProgressIndicator(
                    progress = { status.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${formatBytes(status.downloadedBytes)} / ${formatBytes(status.totalBytes)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!status.isModelAvailable) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Gemma 4 model required",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Button(onClick = onDownloadModel) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Download")
                    }
                }
                Text(
                    text = "Approx. ${formatBytes(status.totalBytes)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Loading Gemma engine",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmptyChatState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No messages",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(
                    if (isUser) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isUser) "You" else "Gemma",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (message.content.isBlank() && message.isStreaming) "..." else message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    isGenerating: Boolean,
    enabled: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onStopGeneration: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                enabled = enabled && !isGenerating,
                placeholder = { Text(text = "Message Gemma") },
                minLines = 1,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )
            IconButton(
                onClick = if (isGenerating) onStopGeneration else onSend,
                enabled = isGenerating || (enabled && value.isNotBlank())
            ) {
                Icon(
                    imageVector = if (isGenerating) Icons.Filled.Stop else Icons.AutoMirrored.Filled.Send,
                    contentDescription = if (isGenerating) "Stop" else "Send"
                )
            }
        }
    }
}

private fun runtimeStatus(uiState: ChatUiState): String {
    val status = uiState.modelStatus
    return when {
        status.isDownloading -> "Downloading ${downloadPercent(status)}"
        !status.isModelAvailable -> "Model not downloaded"
        uiState.isEngineReady -> "Ready (${uiState.backendName})"
        else -> "Loading model"
    }
}

private fun downloadPercent(status: ModelDownloadStatus): String {
    return "${(status.progress.coerceIn(0f, 1f) * 100).toInt()}%"
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val gib = bytes / 1_073_741_824.0
    if (gib >= 1.0) return "%.2f GB".format(gib)
    return "%.0f MB".format(bytes / 1_048_576.0)
}
