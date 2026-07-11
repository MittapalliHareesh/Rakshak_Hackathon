package com.shubham.healthlens.feature.medicine.domain.usecase

import com.shubham.healthlens.feature.medicine.domain.repository.SpeechOutputRepository
import javax.inject.Inject

class SpeakAnswerUseCase @Inject constructor(
    private val repository: SpeechOutputRepository
) {
    operator fun invoke(text: String, languageTag: String) {
        repository.speak(text, languageTag)
    }

    fun stop() = repository.stop()
}
