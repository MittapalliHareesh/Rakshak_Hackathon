package com.shubham.healthlens.feature.medicine.domain.usecase

import com.shubham.healthlens.feature.medicine.domain.repository.MedicineAssistantRepository
import javax.inject.Inject

class PrepareMedicineModelUseCase @Inject constructor(
    private val repository: MedicineAssistantRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.prepareModel()
}

class DownloadMedicineModelUseCase @Inject constructor(
    private val repository: MedicineAssistantRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.downloadModel()
}

class ResetMedicineConversationUseCase @Inject constructor(
    private val repository: MedicineAssistantRepository
) {
    suspend operator fun invoke() = repository.resetConversation()
}
