package com.shubham.healthlens.feature.medicine.domain.repository

interface OcrRepository {
    suspend fun recognize(imagePath: String): Result<String>
}
