package com.shubham.ondevicerag.feature.chat.domain.model

data class ModelDownloadStatus(
    val isModelAvailable: Boolean = false,
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L
)
