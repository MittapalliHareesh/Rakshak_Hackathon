package com.androidblunders.rakshak.orchestrator

import android.util.Log
import com.androidblunders.rakshak.core.contract.MessageSource
import com.androidblunders.rakshak.core.contract.SpeechToTextEngine
import com.androidblunders.rakshak.core.contract.ThreatResponder
import com.androidblunders.rakshak.core.model.CallContext
import com.androidblunders.rakshak.core.model.MessagePayload
import com.androidblunders.rakshak.core.model.ThreatLevel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * The central nervous system. It wires the plug-and-play modules together:
 *
 *  message sources ─┐
 *  call transcript ─┼─► SessionManager (rolling context)
 *  STT stream ──────┘            │
 *                                ▼
 *                        ThreatFusionEngine.evaluate()  ──► StateFlow<ThreatLevel>
 *                                                              │ (observed)
 *                                                              ▼
 *                                                        ThreatResponder(s)
 *
 * The orchestrator itself holds NO business rules about individual models — it
 * only coordinates. Swapping any module (real vs mock) never touches this class.
 */
@Singleton
class RakshakOrchestrator @Inject constructor(
    private val fusionEngine: ThreatFusionEngine,
    private val sessionManager: SessionManager,
    private val messageSources: Set<@JvmSuppressWildcards MessageSource>,
    private val speechToText: SpeechToTextEngine,
    private val responders: Set<@JvmSuppressWildcards ThreatResponder>,
) {
    /** UI observes this directly (Dashboard, overlays). Single source of truth. */
    val threatState: StateFlow<ThreatLevel> = fusionEngine.currentThreatLevel

    private val scope = CoroutineScope(SupervisorJob())
    private var started = false

    /** Call once from the foreground service / Application to begin monitoring. */
    fun start() {
        if (started) return
        started = true

        // 1. Drive responders off every distinct threat-level transition.
        scope.launch {
            threatState
                .onEach { level -> dispatchToResponders(level) }
                .collect {}
        }

        // 2. Fan-in all message sources into the analysis pipeline.
        if (messageSources.isNotEmpty()) {
            scope.launch {
                messageSources.map { it.incomingMessages }
                    .merge()
                    .collect { onMessage(it) }
            }
            messageSources.forEach { it.startListening() }
        }

        // 3. Feed STT transcripts as remote turns + detect the guidance cheat-code.
        scope.launch {
            speechToText.transcriptions.collect { onTranscript(it) }
        }
        speechToText.start()

        Log.i(TAG, "Orchestrator started with ${messageSources.size} source(s), " +
                "${responders.size} responder(s).")
    }

    fun stop() {
        if (!started) return
        started = false
        messageSources.forEach { it.stopListening() }
        speechToText.stop()
        fusionEngine.reset()
    }

    private suspend fun onMessage(payload: MessagePayload) {
        if (!sessionManager.preferences.value.protectionEnabled) return
        sessionManager.startSession(payload.channel, payload.sender)
        val context = sessionManager.appendTurn(
            CallContext.Turn(speaker = CallContext.Speaker.REMOTE, text = payload.body),
        )
        fusionEngine.evaluate(context)
    }

    private suspend fun onTranscript(text: String) {
        if (!sessionManager.preferences.value.protectionEnabled) return

        // Cheat-code check first: the user asking for help overrides everything.
        if (matchesCheatCode(text)) {
            Log.i(TAG, "Guidance cheat-code detected -> GENTLE_GUIDANCE")
            fusionEngine.override(ThreatLevel.GENTLE_GUIDANCE)
            return
        }

        val context = sessionManager.appendTurn(
            CallContext.Turn(speaker = CallContext.Speaker.REMOTE, text = text),
        )
        fusionEngine.evaluate(context)
    }

    private fun matchesCheatCode(text: String): Boolean {
        val code = sessionManager.preferences.value.guidanceCheatCode.trim().lowercase()
        if (code.isEmpty()) return false
        return text.lowercase().contains(code)
    }

    private suspend fun dispatchToResponders(level: ThreatLevel) {
        responders.forEach { responder ->
            try {
                responder.onThreatLevel(level)
            } catch (t: Throwable) {
                // Fault tolerance: a failing responder (e.g. overlay BadTokenException)
                // must never take down the pipeline; siblings still run.
                Log.e(TAG, "Responder ${responder::class.simpleName} failed for $level", t)
            }
        }
    }

    private companion object {
        const val TAG = "RakshakOrchestrator"
    }
}
