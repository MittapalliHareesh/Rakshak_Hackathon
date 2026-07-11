package com.androidblunders.rakshak.spam_detection

// =============================================================================
//  ENUMS — shared vocabulary across all modules
// =============================================================================

/** Direction of the call session being analysed. */
enum class CallDirection { INCOMING, OUTGOING, UNKNOWN }

/** Network connectivity at the time of analysis. */
enum class NetworkState { WIFI, MOBILE_DATA, OFFLINE, UNKNOWN }

/** Scam lifecycle stage inferred by the state machine / AI. */
enum class ConversationStage {
    UNKNOWN,
    INTRO,
    AUTHORITY_ESTABLISHMENT,
    FEAR_INDUCTION,
    THREAT_DELIVERY,
    ISOLATION,
    FINANCIAL_EXTRACTION,
    RESOLUTION          // post-extraction cool-down (rare)
}

/** Overall direction the risk score is moving. */
enum class RiskTrend { RISING, FALLING, STABLE, SPIKE }

/** Actions the InterventionEngine can take once a threshold is crossed. */
enum class InterventionAction {
    MONITOR,            // ≥40  — silent status indicator
    DISPLAY_WARNING,    // ≥65  — banner notification
    PLAY_AUDIO_ALERT,   // ≥80  — audible beep + spoken caution
    INJECT_VOICE,       // ≥90  — TTS over speaker (not routed to caller)
    HARD_LOCK,          // ≥100 — full-screen modal + optional auto-disconnect
    NONE
}

// =============================================================================
//  INPUT CONTRACTS — immutable data classes the host feeds into the pipeline
// =============================================================================

/**
 * Metadata about the active call session.
 * Provided once via [SpamGuardSession.start].
 *
 * @param callerNumber    Raw E.164 or local phone number string.
 * @param isKnownContact  True if the number exists in the device contacts.
 *                        When true the RiskAggregator caps scores at 30
 *                        unless financial keywords are detected.
 * @param callDirection   INCOMING / OUTGOING / UNKNOWN.
 * @param callStartTimeMs Unix epoch milliseconds when the call connected.
 * @param simSlot         SIM slot index (0 or 1) for dual-SIM awareness.
 * @param carrierName     Network carrier name, e.g. "Jio", "Airtel".
 */
data class CallMetadata(
    val callerNumber: String,
    val isKnownContact: Boolean = false,
    val callDirection: CallDirection = CallDirection.INCOMING,
    val callStartTimeMs: Long = System.currentTimeMillis(),
    val simSlot: Int = 0,
    val carrierName: String? = null
)

/**
 * A single SMS / notification message.
 * Multiple messages arrive over time and are collected in [CallContext.recentSmsMessages].
 *
 * @param sender          Phone number or short code that sent the SMS.
 * @param body            Full raw text of the message.
 * @param packageName     Source package (e.g. "com.android.mms", "com.whatsapp").
 * @param receivedAtMs    Unix epoch ms when the message was received.
 * @param isOtp           True when the message body matches OTP patterns
 *                        (auto-detected by the SMS normaliser).
 * @param extractedUpi    Any UPI ID found in the message body, e.g. "attacker@upi".
 * @param containsLink    True if a URL / shortened link was found in the body.
 */
data class SMSMessage(
    val sender: String,
    val body: String,
    val packageName: String,
    val receivedAtMs: Long = System.currentTimeMillis(),
    val isOtp: Boolean = false,
    val extractedUpi: String? = null,
    val containsLink: Boolean = false
)

/**
 * A single unit of speech-to-text output for the ongoing call.
 * The rolling list of segments forms the conversation transcript fed to the AI.
 *
 * @param speaker         "USER" or "CALLER" — who said this.
 * @param text            Normalised transcribed text (after HinglishNormaliser).
 * @param rawText         Original, un-normalised text for debugging.
 * @param timestampMs     Wall-clock time when this segment was captured.
 * @param wordCount       Pre-computed word count for WPM calculations.
 * @param confidenceScore STT confidence [0.0, 1.0].
 */
data class TranscriptSegment(
    val speaker: String,
    val text: String,
    val rawText: String = text,
    val timestampMs: Long = System.currentTimeMillis(),
    val wordCount: Int = text.split("\\s+".toRegex()).filter { it.isNotBlank() }.size,
    val confidenceScore: Float = 1.0f
)

/**
 * Acoustic / prosodic features extracted from the raw audio stream.
 * Calculated by the AcousticFeatureExtractor; immutable snapshot.
 *
 * @param callerTalkRatio     Fraction of call time the caller was speaking [0.0, 1.0].
 * @param callerWordsPerMin   Estimated WPM for the caller.
 * @param userWordsPerMin     Estimated WPM for the user.
 * @param interruptionCount   Number of overlapping-speech events detected.
 * @param silencePeriodsSec   Total seconds of silence (possible intimidation tactic).
 * @param avgPitchHz          Rough average pitch of caller's voice (Hz), if available.
 */
data class AcousticFeatures(
    val callerTalkRatio: Float = 0f,
    val callerWordsPerMin: Float = 0f,
    val userWordsPerMin: Float = 0f,
    val interruptionCount: Int = 0,
    val silencePeriodsSec: Float = 0f,
    val avgPitchHz: Float? = null
)

/**
 * Snapshot of the device's runtime state at analysis time.
 * Contextual signals that help the risk engine adjust thresholds.
 *
 * @param networkState      Current connectivity (WIFI / MOBILE_DATA / OFFLINE).
 * @param batteryPercent    Battery level [0–100].
 * @param isScreenOn        Whether the device screen is on.
 * @param locationCity      Coarse city-level location (no GPS coordinates stored).
 * @param activeAppPackage  Foreground app at time of analysis (detects screen-share scams).
 * @param isDozeMode        Whether the device is in Doze / standby mode.
 */
data class DeviceContext(
    val networkState: NetworkState = NetworkState.UNKNOWN,
    val batteryPercent: Int = -1,
    val isScreenOn: Boolean = true,
    val locationCity: String? = null,
    val activeAppPackage: String? = null,
    val isDozeMode: Boolean = false
)

// =============================================================================
//  CALL CONTEXT — the single object fed into every ThreatAnalyzer
// =============================================================================

/**
 * The complete, rich input bundle passed to [ThreatAnalyzer.analyze].
 *
 * Aggregates ALL available signals for a single analysis pass:
 *   • Call-level metadata (who is calling, known contact flag)
 *   • Conversation transcript (rolling list, max 20 segments)
 *   • Recent SMS / notification messages (list, not just one body)
 *   • Device context (network state, battery, foreground app)
 *   • Acoustic features (talk ratio, WPM, interruptions)
 *
 * @param callMetadata        Static facts about the call session.
 * @param transcriptSegments  Ordered list of STT segments (oldest first, max 20).
 * @param recentSmsMessages   SMS/notifications received during or before the call.
 * @param deviceContext       Runtime device snapshot at analysis time.
 * @param acousticFeatures    Prosodic features from the audio stream.
 * @param analysisTimestampMs When this snapshot was assembled (for latency tracking).
 */
data class CallContext(
    val callMetadata: CallMetadata,
    val transcriptSegments: List<TranscriptSegment> = emptyList(),
    val recentSmsMessages: List<SMSMessage> = emptyList(),
    val deviceContext: DeviceContext = DeviceContext(),
    val acousticFeatures: AcousticFeatures = AcousticFeatures(),
    val analysisTimestampMs: Long = System.currentTimeMillis()
) {
    // -------------------------------------------------------------------------
    // Convenience accessors (avoids null-checks at call sites)
    // -------------------------------------------------------------------------

    /** The caller's phone number. */
    val callerNumber: String get() = callMetadata.callerNumber

    /** True if the caller is in the device contacts. */
    val isKnownContact: Boolean get() = callMetadata.isKnownContact

    /** Full transcript as a single string (oldest segment first). */
    val fullTranscriptText: String get() =
        transcriptSegments.joinToString("\n") { "[${it.speaker}] ${it.text}" }

    /** Bodies of all recent SMS messages joined for quick keyword scanning. */
    val allSmsText: String get() =
        recentSmsMessages.joinToString("\n") { "[${it.sender}] ${it.body}" }

    /** True if any recent SMS contains an OTP — triggers SMSBoost in the risk engine. */
    val hasOtpSms: Boolean get() = recentSmsMessages.any { it.isOtp }

    /** True if any recent SMS contains a UPI ID — triggers UPI extortion signal. */
    val hasUpiSms: Boolean get() = recentSmsMessages.any { it.extractedUpi != null }

    /** Duration of the call in milliseconds (0 if metadata is unavailable). */
    val callDurationMs: Long get() =
        analysisTimestampMs - callMetadata.callStartTimeMs
}

// =============================================================================
//  OUTPUT CONTRACTS — what the pipeline produces
// =============================================================================

/**
 * Per-analyzer output: a normalised threat probability + classification.
 *
 * @param score      Normalised threat probability [0.0, 1.0].
 * @param label      Best classification: SAFE | SUSPICIOUS | PHISHING | SCAM | MALWARE.
 * @param confidence How certain the model is [0.0, 1.0].
 * @param signals    Named signal categories that contributed to this score
 *                   (e.g. "UPI_EXTORTION", "URGENCY_KEYWORD", "OTP_CORRELATION").
 * @param stage      Inferred conversation stage if detected by the model.
 * @param rawOutput  Verbatim model response for debugging / audit.
 */
data class ThreatScore(
    val score: Float,
    val label: String,
    val confidence: Float,
    val signals: List<String> = emptyList(),
    val stage: ConversationStage = ConversationStage.UNKNOWN,
    val rawOutput: String = ""
)

/**
 * The fused, aggregated risk state emitted by the [RiskAggregator].
 * This is what the host app / UI observes via [SpamGuardSession.riskState].
 *
 * @param currentScore        Aggregated risk score [0–100] (note: 0-100, not 0.0-1.0).
 * @param trend               How the score is moving over time.
 * @param timeAtCurrentLevelMs Milliseconds the score has been at or above
 *                            the current [InterventionAction] threshold.
 * @param dominantSignals     Top signal names driving the score.
 * @param recommendedAction   The intervention tier matching [currentScore].
 * @param stage               Current inferred conversation stage.
 * @param isKnownContactCapped True if the score was capped at 30 due to known-contact rule.
 */
data class AggregatedRiskState(
    val currentScore: Float,
    val trend: RiskTrend,
    val timeAtCurrentLevelMs: Long,
    val dominantSignals: List<String>,
    val recommendedAction: InterventionAction,
    val stage: ConversationStage = ConversationStage.UNKNOWN,
    val isKnownContactCapped: Boolean = false
)

/**
 * Events emitted by the InterventionEngine to the host app.
 * Sealed so the host can exhaustively handle all cases.
 */
sealed class InterventionEvent {
    /** The action that triggered this event. */
    abstract val action: InterventionAction
    /** The risk score at time of event. */
    abstract val triggerScore: Float

    data class Monitor(
        override val action: InterventionAction = InterventionAction.MONITOR,
        override val triggerScore: Float
    ) : InterventionEvent()

    data class DisplayWarning(
        override val action: InterventionAction = InterventionAction.DISPLAY_WARNING,
        override val triggerScore: Float,
        val message: String
    ) : InterventionEvent()

    data class PlayAudioAlert(
        override val action: InterventionAction = InterventionAction.PLAY_AUDIO_ALERT,
        override val triggerScore: Float
    ) : InterventionEvent()

    data class InjectVoice(
        override val action: InterventionAction = InterventionAction.INJECT_VOICE,
        override val triggerScore: Float,
        val ttsText: String
    ) : InterventionEvent()

    data class HardLock(
        override val action: InterventionAction = InterventionAction.HARD_LOCK,
        override val triggerScore: Float,
        val autoDisconnectAfterMs: Long = 10_000L
    ) : InterventionEvent()
}

// =============================================================================
//  THREAT ANALYZER INTERFACE
// =============================================================================

/**
 * Unified contract every threat-analysis model must satisfy.
 *
 * Implementations:
 *  - [GemmaAnalyzer]       — on-device, offline via MediaPipe LLM Inference API
 *  - GeminiLiveAnalyzer    — (future) cloud WebSocket for deeper contextual analysis
 *  - RegexRuleAnalyzer     — (future) fast deterministic rule-set for known patterns
 *
 * New engines can be registered in [SpamDetectionModule] with a single @Binds line.
 */
interface ThreatAnalyzer {
    /**
     * Analyses [context] and returns a [ThreatScore].
     * Safe to call from any coroutine context; implementations must
     * switch to an appropriate dispatcher internally.
     */
    suspend fun analyze(context: CallContext): ThreatScore
}