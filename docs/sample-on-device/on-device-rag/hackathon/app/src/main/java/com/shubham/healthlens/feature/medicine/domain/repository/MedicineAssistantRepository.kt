package com.shubham.healthlens.feature.medicine.domain.repository

import com.shubham.healthlens.feature.medicine.domain.model.MedicineContext
import com.shubham.healthlens.feature.medicine.domain.model.MedicineMessage
import com.shubham.healthlens.feature.medicine.domain.model.ModelStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MedicineAssistantRepository {
    val modelStatus: StateFlow<ModelStatus>

    suspend fun prepareModel(): Result<Unit>

    suspend fun downloadModel(): Result<Unit>

    fun ask(
        context: MedicineContext,
        question: String,
        history: List<MedicineMessage>
    ): Flow<String>

    suspend fun resetConversation()
}
