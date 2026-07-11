package com.shubham.healthlens.feature.medicine.domain.model

data class ModelStatus(
    val isAvailable: Boolean = false,
    val isDownloading: Boolean = false,
    val isReady: Boolean = false,
    val progress: Float = 0f,
    val backend: String = "CPU"
)
