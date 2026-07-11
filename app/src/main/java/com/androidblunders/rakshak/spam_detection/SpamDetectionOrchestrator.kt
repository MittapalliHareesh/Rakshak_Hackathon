package com.androidblunders.rakshak.spam_detection

import android.util.Log
import com.androidblunders.rakshak.call.LiveTranscript
import com.androidblunders.rakshak.call.LiveTranscriptBus
import com.androidblunders.rakshak.core.model.ThreatLevel
import com.androidblunders.rakshak.messaging.MessageData
import com.androidblunders.rakshak.messaging.MessageExtractor
import com.androidblunders.rakshak.orchestrator.ThreatFusionEngine as CoreThreatState
import javax.inject.Inject
import javax.inject.Singleton
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

<<<<<<< HEAD
/** UI-facing result of a single analyzed message. */
=======
/** UI-facing result of a single analysis pass. */
>>>>>>> 6fc7644e50134b1fb57acc8b7aad2bbd14d502a3
data class SpamDetectionResult(
    val sender: String,
    val messageBody: String,
    val score: ThreatScore,
    val status: String,
    val timestamp: Long,
<<<<<<< HEAD
    val hasTransaction: Boolean = false
=======
>>>>>>> 6fc7644e50134b1fb57acc8b7aad2bbd14d502a3
)

/**
 * The pipeline entry-point. Fuses two input sources into the rich [CallContext]
 * consumed by [ThreatFusionEngine] → [GemmaAnalyzer]:
 *
 *  1. [MessageExtractor.messageFlow] — inbound SMS / chat notifications.
 *  2. [LiveTranscriptBus] — live call speech-to-text (VOICE / STT module).
 *
 * Both maintain rolling buffers so the model sees short-term context. Every
 * result is published to [latestResult] (for the UI) and mapped into the
 * app-wide core [ThreatLevel] so the overlay + TTS responders react — a single
 * source of truth.
 */
@Singleton
class SpamDetectionOrchestrator @Inject constructor(
    private val fusionEngine: ThreatFusionEngine,
    private val coreThreatState: CoreThreatState,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val bufferMutex = Mutex()

    private val smsBuffer = ArrayDeque<SMSMessage>()
    private val transcriptBuffer = ArrayDeque<TranscriptSegment>()

    private val _latestResult = MutableStateFlow<SpamDetectionResult?>(null)
    val latestResult: StateFlow<SpamDetectionResult?> = _latestResult.asStateFlow()

    private val _recentResults = MutableStateFlow<List<SpamDetectionResult>>(emptyList())
    val recentResults: StateFlow<List<SpamDetectionResult>> = _recentResults.asStateFlow()

    /** Start observing both input sources. Idempotent enough for app start-up. */
    fun startObserving() {
        Log.i(TAG, "Starting spam detection pipeline…")

        MessageExtractor.messageFlow
            .onEach { onMessage(it) }
            .catch { e -> Log.e(TAG, "messageFlow error — recovering", e) }
            .launchIn(scope)

        LiveTranscriptBus.transcripts
            .onEach { onTranscript(it) }
            .catch { e -> Log.e(TAG, "transcript flow error — recovering", e) }
            .launchIn(scope)
    }

    /** Direct SMS injection (e.g. from a future SMS BroadcastReceiver). */
    fun pushSms(sms: SMSMessage) {
        scope.launch {
            val context = bufferMutex.withLock {
                smsBuffer.addLast(sms)
                while (smsBuffer.size > MAX_BUFFER) smsBuffer.removeFirst()
                buildSmsContext()
            }
            evaluate(context, sender = sms.sender, body = sms.body)
        }
    }

    // ---- inputs --------------------------------------------------------------

    private fun onMessage(message: MessageData) {
        scope.launch {
            val sms = message.toSms()
            val context = bufferMutex.withLock {
                smsBuffer.addLast(sms)
                while (smsBuffer.size > MAX_BUFFER) smsBuffer.removeFirst()
                buildSmsContext()
            }
            evaluate(context, sender = sms.sender, body = sms.body)
        }
<<<<<<< HEAD

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
=======
>>>>>>> 6fc7644e50134b1fb57acc8b7aad2bbd14d502a3
    }

    private fun onTranscript(transcript: LiveTranscript) {
        scope.launch {
            val segment = TranscriptSegment(speaker = transcript.speaker, text = transcript.text)
            val context = bufferMutex.withLock {
                transcriptBuffer.addLast(segment)
                while (transcriptBuffer.size > MAX_BUFFER) transcriptBuffer.removeFirst()
                buildTranscriptContext()
            }
            evaluate(context, sender = transcript.speaker, body = transcript.text)
        }
    }

<<<<<<< HEAD
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
=======
    // ---- core ----------------------------------------------------------------

    private suspend fun evaluate(context: CallContext, sender: String, body: String) {
        try {
            val score = fusionEngine.evaluate(context)
            onThreatScored(sender, body, score)
        } catch (e: Exception) {
            Log.e(TAG, "Analysis failed for $sender", e)
        }
    }

    private fun onThreatScored(sender: String, body: String, score: ThreatScore) {
        val status = statusFor(score.score)
        val result = SpamDetectionResult(sender, body, score, status, System.currentTimeMillis())
        _latestResult.value = result
        _recentResults.value = (listOf(result) + _recentResults.value).take(MAX_BUFFER)

        // Align with core: drive the app-wide ThreatLevel (overlay + TTS + UI react).
        coreThreatState.override(mapToThreatLevel(score.score))

        Log.i(TAG, "Spam result: $sender score=${(score.score * 100).toInt()}% " +
                "label=${score.label} stage=${score.stage} status=$status")
    }

    // ---- helpers -------------------------------------------------------------

    private fun buildSmsContext(): CallContext = CallContext(
        callMetadata = CallMetadata(callerNumber = smsBuffer.lastOrNull()?.sender ?: "unknown"),
        recentSmsMessages = smsBuffer.toList(),
>>>>>>> 6fc7644e50134b1fb57acc8b7aad2bbd14d502a3
    )

    private fun buildTranscriptContext(): CallContext = CallContext(
        callMetadata = CallMetadata(callerNumber = "Live Call"),
        transcriptSegments = transcriptBuffer.toList(),
    )

    private fun MessageData.toSms() = SMSMessage(
        sender = sender,
        body = content,
        packageName = packageName,
        receivedAtMs = timestamp,
        isOtp = OTP_REGEX.containsMatchIn(content),
        extractedUpi = UPI_REGEX.find(content)?.value,
        containsLink = LINK_REGEX.containsMatchIn(content),
    )

    private fun statusFor(score: Float): String = when {
        score >= THRESHOLD_ALERT      -> "🚨 ALERT"
        score >= THRESHOLD_WARN       -> "⚠️ WARN"
        score >= THRESHOLD_SUSPICIOUS -> "🟡 SUSPICIOUS"
        else                          -> "✅ SAFE"
    }

    /** Same thresholds as the core fusion engine. */
    private fun mapToThreatLevel(score: Float): ThreatLevel = when {
        score >= 0.92f -> ThreatLevel.EMERGENCY
        score >= 0.75f -> ThreatLevel.ACTIVE_THREAT
        score >= 0.55f -> ThreatLevel.MEDIUM
        score >= 0.30f -> ThreatLevel.LOW
        else           -> ThreatLevel.IDLE
    }

    private companion object {
        const val TAG = "SpamDetectionOrchestrator"
        const val MAX_BUFFER = 25
        const val THRESHOLD_SUSPICIOUS = 0.35f
        const val THRESHOLD_WARN = 0.60f
        const val THRESHOLD_ALERT = 0.80f

        val OTP_REGEX = Regex("""\b\d{4,8}\b.*(otp|code|password)|(otp|code|password).*\b\d{4,8}\b""", RegexOption.IGNORE_CASE)
        val UPI_REGEX = Regex("""[a-zA-Z0-9._-]+@[a-zA-Z]{2,}""")
        val LINK_REGEX = Regex("""https?://|\bwww\.|\b[a-z0-9-]+\.(com|in|net|org|xyz|link|ly)\b""", RegexOption.IGNORE_CASE)
    }
}
