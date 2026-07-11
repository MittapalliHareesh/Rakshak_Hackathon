package com.shubham.healthlens.feature.medicine.domain.usecase

import com.shubham.healthlens.feature.medicine.domain.model.MedicineContext
import com.shubham.healthlens.feature.medicine.domain.model.MedicineMessage
import com.shubham.healthlens.feature.medicine.domain.repository.MedicineAssistantRepository
import javax.inject.Inject

class AskMedicineUseCase @Inject constructor(
    private val repository: MedicineAssistantRepository
) {
    operator fun invoke(
        context: MedicineContext,
        question: String,
        history: List<MedicineMessage>
    ) = repository.ask(context, question.trim(), history)
}
