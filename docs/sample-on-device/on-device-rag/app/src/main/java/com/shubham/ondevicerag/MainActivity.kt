package com.shubham.ondevicerag

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.shubham.ondevicerag.feature.chat.presentation.ChatScreen
import com.shubham.ondevicerag.feature.chat.presentation.ChatViewModel
import com.shubham.ondevicerag.ui.theme.OnDeviceRagTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnDeviceRagTheme {
                val viewModel: ChatViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()

                ChatScreen(
                    uiState = uiState,
                    onInputChange = viewModel::onInputChange,
                    onSend = viewModel::sendMessage,
                    onStopGeneration = viewModel::stopGeneration,
                    onDownloadModel = viewModel::downloadModel,
                    onNewChat = viewModel::newChat
                )
            }
        }
    }
}
