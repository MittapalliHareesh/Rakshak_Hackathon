package com.shubham.healthlens.feature.medicine.domain.repository

interface SpeechOutputRepository {
    fun speak(text: String, languageTag: String)

    fun stop()
}
