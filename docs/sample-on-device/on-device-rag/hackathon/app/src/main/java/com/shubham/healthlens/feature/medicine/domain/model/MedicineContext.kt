package com.shubham.healthlens.feature.medicine.domain.model

data class MedicineContext(
    val imagePath: String? = null,
    val recognizedText: String = ""
) {
    val hasEvidence: Boolean
        get() = !imagePath.isNullOrBlank() || recognizedText.isNotBlank()
}
