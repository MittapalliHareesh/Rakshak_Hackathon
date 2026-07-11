package com.androidblunders.rakshak.spam_detection

import android.util.Log
import com.androidblunders.rakshak.call.LiveTranscript
import com.androidblunders.rakshak.call.LiveTranscriptBus
import com.androidblunders.rakshak.core.model.ThreatLevel
import com.androidblunders.rakshak.messaging.MessageData
import com.androidblunders.rakshak.messaging.MessageExtractor
import com.androidblunders.rakshak.orchestrator.ThreatFusionEngine as CoreThreatState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** UI-facing result of a single analyzed message. */
data class SpamDetectionResult(
    val sender: String,
    val messageBody: String,
    val score: ThreatScore,
    val status: String,
    val timestamp: Long,
    val hasTransaction: Boolean = false
)

/**
 * SpamDetectionOrchestrator — the pipeline entry-point for SMS / notification analysis.
 *
 * ## What it does
 * 1. Subscribes to [MessageExtractor.messageFlow] (SharedFlow<MessageData>).
 * 2. Accumulates incoming messages into a **rolling SMS buffer** (max 25).
 * 3. On every new message, assembles a rich [CallContext] from the buffer
 *    (SMS list, device context, acoustic defaults) and sends it through
 *    the [ThreatFusionEngine].
 * 4. Logs the result (with TODO hooks for Room / StateFlow / notifications).
 *
 * ## Lifecycle
 * This singleton lives for the duration of the app process.
 * Call [startObserving] once from [RakshakApplication.onCreate].
 *
 * ## Thread safety
 * [smsBuffer] is protected by a [Mutex] — concurrent writes from the
 * SharedFlow collector and future SMS BroadcastReceiver are safe.
 *
 * ## Extending output
 * TODO: Replace the Log calls in [onThreatScored] with:
 *   1. Room DAO insert → security_history table
 *   2. MutableStateFlow<AggregatedRiskState> → SecurityHistoryViewModel
 *   3. NotificationManager alert when risk ≥ THRESHOLD_ALERT
 */
@Singleton
class SpamDetectionOrchestrator @Inject constructor(
    private val fusionEngine: ThreatFusionEngine,
    // The app-wide ThreatLevel state (core). We push our score into it so the
    // overlay/TTS responders and dashboard react — single source of truth.
    private val coreThreatState: CoreThreatState,
) {
    companion object {
        private const val TAG = "SpamDetectionOrchestrator"

        /** Maximum SMS messages kept in the rolling buffer. */
        private const val MAX_SMS_BUFFER = 25

        /** Max recent results kept in memory for the UI StateFlow. */
        private const val MAX_RECENT = 25

        // Risk thresholds (score is 0.0–1.0, multiply by 100 for display)
        private const val THRESHOLD_SUSPICIOUS = 0.35f
        private const val THRESHOLD_WARN       = 0.60f
        private const val THRESHOLD_ALERT      = 0.80f
    }

    /**
     * Process-wide coroutine scope — SupervisorJob ensures a failure in
     * one child coroutine never cancels the whole pipeline.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Begins observing [MessageExtractor.messageFlow].
     * Idempotent — safe to call multiple times; the singleton [scope] ensures
     * a single active collector.
     */
    fun startObserving() {
        Log.i(TAG, "Starting spam detection pipeline…")

        // Source 1: inbound SMS / chat notifications.
        MessageExtractor.messageFlow
            .onEach { message -> processMessage(message) }
            .catch { e -> Log.e(TAG, "messageFlow error — pipeline recovering", e) }
            .launchIn(scope)

        // Source 2: live call speech-to-text (pushed by the STT module).
        LiveTranscriptBus.transcripts
            .onEach { transcript -> analyze(transcript.toCallContext()) }
            .catch { e -> Log.e(TAG, "transcript flow error — pipeline recovering", e) }
            .launchIn(scope)
    }

    /**
     * Pushes an [SMSMessage] directly into the buffer (e.g. from an SMS
     * BroadcastReceiver) and immediately triggers analysis.
     * Can be called from any thread.
     */
    fun pushSms(sms: SMSMessage) {
        scope.launch {
            addToBuffer(sms)
            analyzeCurrentBuffer()
        }
    }

    // -------------------------------------------------------------------------
    // Private pipeline (shared by messages + live transcripts)
    // -------------------------------------------------------------------------

    private fun processMessage(message: MessageData) {
        scope.launch {
            try {
                val context = message.toCallContext()
                Log.d(TAG, "Analysing message from ${context.sender} " +
                        "(${context.messageBody.take(60)}…)")

                val threatScore = fusionEngine.evaluate(context)
                onThreatScored(context, threatScore)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process message from ${message.sender}", e)
            }
        }
    }

    /**
     * Called with the fused [ThreatScore] for every analysis pass.
     * Currently writes a structured log. Hook in Room / StateFlow / Notification here.
     */
    private fun onThreatScored(ctx: CallContext, score: ThreatScore) {
        val scaledScore = score.score

        val level = when {
            scaledScore >= THRESHOLD_INTERVENE  -> "🚨 INTERVENE"
            scaledScore >= THRESHOLD_ALERT      -> "🔴 ALERT"
            scaledScore >= THRESHOLD_WARN       -> "⚠️  WARN"
            scaledScore >= THRESHOLD_SUSPICIOUS -> "🟡 SUSPICIOUS"
            else                                -> "✅ SAFE"
        }

        val latestSms = ctx.recentSmsMessages.lastOrNull()
        val isTransaction = latestSms?.let { com.androidblunders.rakshak.reporting.TransactionDetailsExtractor.isTransactionSms(it.body) } ?: false

        // Publish to StateFlow observers (UI / dashboard).
        val result = SpamDetectionResult(
            sender      = ctx.callerNumber,
            messageBody = latestSms?.body ?: ctx.allSmsText.take(120),
            score       = score,
            status      = level,
            timestamp   = ctx.analysisTimestampMs,
            hasTransaction = isTransaction
        )
        _latestResult.value = result
        _recentResults.value = (listOf(result) + _recentResults.value).take(MAX_RECENT)

        val otpFlag   = if (ctx.hasOtpSms) " [OTP]" else ""
        val upiFlag   = if (ctx.hasUpiSms) " [UPI]" else ""
        val signalStr = if (score.signals.isEmpty()) "none" else score.signals.joinToString()

        Log.i(
            TAG, """
            ┌── Spam Detection Result ──────────────────────────────────
            │  Caller       : ${ctx.callerNumber}
            │  Known contact: ${ctx.isKnownContact}
            │  SMS in buffer: ${ctx.recentSmsMessages.size}$otpFlag$upiFlag
            │  Stage        : ${score.stage}
            │  Score        : ${"%.0f".format(scaledScore * 100)}/100
            │  Label        : ${score.label}
            │  Confidence   : ${"%.1f".format(score.confidence * 100)}%
            │  Signals      : $signalStr
            │  Status       : $level
            └───────────────────────────────────────────────────────────
            """.trimIndent()
        )

        // TODO (Phase 5): Emit to RiskAggregator instead of logging directly
        // TODO (Phase 6): Trigger InterventionEngine based on scaledScore
        // TODO: Insert into Room security_history DAO
    }

    // -------------------------------------------------------------------------
    // Extension helpers
    // -------------------------------------------------------------------------

    /** Converts the messaging layer's [MessageData] to the spam detection [CallContext]. */
    private fun MessageData.toCallContext() = CallContext(
        callMetadata = CallMetadata(
            callerNumber   = sender,
            isKnownContact = false,
            callDirection  = CallDirection.INCOMING,
            callStartTimeMs = timestamp
        ),
        recentSmsMessages  = listOf(SMSMessage(
            sender       = sender,
            body         = content,
            packageName  = packageName,
            receivedAtMs = timestamp
        ))
    )
}
