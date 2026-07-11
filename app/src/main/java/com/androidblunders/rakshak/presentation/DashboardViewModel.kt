package com.androidblunders.rakshak.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidblunders.rakshak.core.model.ThreatLevel
import com.androidblunders.rakshak.gemma.GemmaModelManager
import com.androidblunders.rakshak.gemma.GemmaTextGenerator
import com.androidblunders.rakshak.orchestrator.RakshakOrchestrator
import com.androidblunders.rakshak.orchestrator.ThreatFusionEngine
import com.androidblunders.rakshak.stub.NoOpSpeechToText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val threatLevel: ThreatLevel = ThreatLevel.IDLE,
    val confidence: Float = 0f,
    val modelReady: Boolean = false,
    val backend: String = "CPU",
    val downloadProgress: Float = 0f,
    val isDownloading: Boolean = false,
    val statusLine: String = "Monitoring active",
)

/**
 * Presentation glue. It only OBSERVES orchestrator/fusion/gemma state and exposes
 * two demo actions — it holds no protection logic itself.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    orchestrator: RakshakOrchestrator,
    fusionEngine: ThreatFusionEngine,
    private val gemma: GemmaTextGenerator,
    private val modelManager: GemmaModelManager,
    // Concrete demo STT so we can inject a fake transcript from the UI.
    private val demoStt: NoOpSpeechToText,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        orchestrator.threatState,
        fusionEngine.currentConfidence,
        gemma.isReady,
        gemma.backend,
        modelManager.status,
    ) { level, confidence, ready, backend, model ->
        DashboardUiState(
            threatLevel = level,
            confidence = confidence,
            modelReady = ready,
            backend = backend,
            downloadProgress = model.progress,
            isDownloading = model.isDownloading,
            statusLine = statusLineFor(level, ready),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    /** Load / download the on-device Gemma weights and initialize the engine. */
    fun prepareModel() {
        viewModelScope.launch { gemma.prepare() }
    }

    /** Demo: push a fake inbound message through the full pipeline. */
    fun simulateMessage(text: String) {
        if (text.isBlank()) return
        demoStt.emit(text)
    }

    private fun statusLineFor(level: ThreatLevel, ready: Boolean): String = when {
        !ready -> "Offline model not loaded — tap Load Gemma"
        level == ThreatLevel.IDLE -> "Monitoring active"
        else -> "Threat detected: $level"
    }
}
