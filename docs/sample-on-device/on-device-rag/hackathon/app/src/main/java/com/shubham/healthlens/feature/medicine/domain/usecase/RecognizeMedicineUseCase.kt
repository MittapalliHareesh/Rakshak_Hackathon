package com.shubham.healthlens.feature.medicine.domain.usecase

import com.shubham.healthlens.feature.medicine.domain.repository.OcrRepository
import javax.inject.Inject

class RecognizeMedicineUseCase @Inject constructor(
    private val repository: OcrRepository
) {
    suspend operator fun invoke(imagePath: String): Result<String> = repository.recognize(imagePath)
}
