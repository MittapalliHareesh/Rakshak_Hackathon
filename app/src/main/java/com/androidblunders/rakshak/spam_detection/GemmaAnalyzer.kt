package com.androidblunders.rakshak.spam_detection

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.LinkedList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device threat analyzer powered by the Gemma 4 model via
 * the MediaPipe LLM Inference API.
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  MODEL SETUP                                                │
 * │  Place the Gemma .task file at:                             │
 * │      app/src/main/assets/gemma.task                         │
 * │                                                             │
 * │  TODO: If the model is not bundled in assets, implement     │
 * │        a download-on-first-run strategy here.               │
 * └─────────────────────────────────────────────────────────────┘
 *
 * Context window: The last [MAX_TURNS] transcript turns are kept in a
 * rolling deque so the model retains short-term conversational memory
 * across repeated calls during the same session.
 */
@Singleton
class GemmaAnalyzer @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ThreatAnalyzer {

    companion object {
        private const val TAG = "GemmaAnalyzer"

        /** Path to the Gemma .task file inside the app's assets folder. */
        private const val MODEL_ASSET_PATH = "gemma.task"

        /**
         * Maximum number of previous transcript turns kept in the rolling
         * context window (architecture: max 20 segments / last 5 minutes).
         */
        private const val MAX_TURNS = 20

        /**
         * Prompt template.
         * The model is instructed to reply with ONLY a JSON object so
         * we can parse the score deterministically.
         *
         * Output JSON fields:
         *  score            – float 0.0-1.0
         *  label            – SAFE | SUSPICIOUS | PHISHING | SCAM | MALWARE
         *  confidence       – float 0.0-1.0
         *  signals          – list of named signals (e.g. "UPI_EXTORTION")
         *  stage            – UNKNOWN | INTRO | AUTHORITY_ESTABLISHMENT |
         *                     FEAR_INDUCTION | THREAT_DELIVERY | ISOLATION |
         *                     FINANCIAL_EXTRACTION | RESOLUTION
         *  reason           – one concise sentence
         */
        private val SYSTEM_PROMPT = """
You are Rakshak, an AI security assistant specialised in detecting phone and SMS-based scams, phishing, and fraud targeting Indian mobile users. You understand English, Hindi, and Hinglish.

Analyse the following call/message context holistically — including the transcript, recent SMS messages, acoustic signals, and device state — and respond with ONLY a valid JSON object (no markdown, no explanation):

{
  "score": <float 0.0-1.0>,
  "label": "<SAFE|SUSPICIOUS|PHISHING|SCAM|MALWARE>",
  "confidence": <float 0.0-1.0>,
  "signals": ["<SIGNAL_1>", "<SIGNAL_2>"],
  "stage": "<UNKNOWN|INTRO|AUTHORITY_ESTABLISHMENT|FEAR_INDUCTION|THREAT_DELIVERY|ISOLATION|FINANCIAL_EXTRACTION|RESOLUTION>",
  "reason": "<one concise sentence>"
}

score:   0.0 = completely safe, 1.0 = definitely malicious
signals: Named threat signals you detected. Use values like:
         UPI_EXTORTION, OTP_CORRELATION, URGENCY_KEYWORD, AUTHORITY_IMPERSONATION,
         DIGITAL_ARREST, PAYMENT_LINK, MALWARE_APK, PHISHING_LINK, UNKNOWN_CALLER,
         RAPID_SPEECH, HIGH_INTERRUPTIONS, SHORT_CALL_SPIKE

Known-contact rule: if isKnownContact=true, be conservative — only raise score above 0.30 if strong financial extraction signals are present.

""".trimIndent()
    }

    // Rolling window of the last MAX_TURNS transcript turns fed to the model.
    private val conversationWindow = LinkedList<String>()

    // Lazily-initialised MediaPipe LLM session — created once on first use.
    private var llmInference: LlmInference? = null

    // -------------------------------------------------------------------------
    // ThreatAnalyzer implementation
    // -------------------------------------------------------------------------

    override suspend fun analyze(context: CallContext): ThreatScore =
        withContext(Dispatchers.IO) {
            try {
                val inference = getOrCreateInference()

                // Sync the rolling window with the latest transcript segments
                syncConversationWindow(context.transcriptSegments)

                val prompt = buildPrompt(context)
                Log.d(TAG, "Sending prompt to Gemma for caller=${context.callerNumber} " +
                        "(${context.transcriptSegments.size} transcript segments, " +
                        "${context.recentSmsMessages.size} SMS messages)")

                val rawResponse = inference.generateResponse(prompt)
                Log.d(TAG, "Gemma raw response: $rawResponse")

                parseResponse(rawResponse)
            } catch (e: Exception) {
                Log.e(TAG, "GemmaAnalyzer failed — returning safe default", e)
                ThreatScore(score = 0f, label = "UNKNOWN", confidence = 0f,
                    rawOutput = e.message ?: "error")
            }
        }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the existing [LlmInference] session or creates one from the
     * model bundled in assets.
     *
     * TODO: If you want to support runtime-downloaded models, replace
     *       [LlmInferenceOptions.builder().setModelAssetPath] with the
     *       absolute path to the downloaded file in internal storage.
     */
    private fun getOrCreateInference(): LlmInference {
        llmInference?.let { return it }

        val options = LlmInferenceOptions.builder()
            .setModelAssetPath(MODEL_ASSET_PATH)
            .setMaxTokens(2048)
            .setTopK(40)
            .setTemperature(0.1f)   // Low temperature → more deterministic JSON output
            .setRandomSeed(42)
            .build()

        return LlmInference.createFromOptions(appContext, options).also {
            llmInference = it
            Log.i(TAG, "LlmInference session created from assets/$MODEL_ASSET_PATH")
        }
    }

    /**
     * Rebuilds the rolling conversation window from the latest [segments].
     * Keeps the last [MAX_TURNS] entries; older ones are evicted.
     */
    private fun syncConversationWindow(segments: List<TranscriptSegment>) {
        conversationWindow.clear()
        val recent = if (segments.size > MAX_TURNS) segments.takeLast(MAX_TURNS) else segments
        recent.forEach { seg ->
            conversationWindow.addLast("[${seg.speaker}] ${seg.text}")
        }
    }

    /**
     * Builds the full prompt from:
     *   1. System instructions
     *   2. Call & caller metadata
     *   3. SMS messages (all recent, newest last)
     *   4. Conversation transcript (rolling window)
     *   5. Acoustic features
     *   6. Device context
     */
    private fun buildPrompt(ctx: CallContext): String = buildString {
        append(SYSTEM_PROMPT)
        append("\n")

        // --- Call metadata ---
        appendLine("=== CALL METADATA ===")
        appendLine("Caller       : ${ctx.callerNumber}")
        appendLine("Known contact: ${ctx.isKnownContact}")
        appendLine("Direction    : ${ctx.callMetadata.callDirection}")
        appendLine("Duration     : ${ctx.callDurationMs / 1000}s")
        ctx.callMetadata.carrierName?.let { appendLine("Carrier      : $it") }
        appendLine()

        // --- SMS messages ---
        if (ctx.recentSmsMessages.isNotEmpty()) {
            appendLine("=== RECENT SMS / NOTIFICATIONS (${ctx.recentSmsMessages.size}) ===")
            ctx.recentSmsMessages.forEachIndexed { i, sms ->
                appendLine("SMS[${i + 1}] from ${sms.sender} at ${sms.receivedAtMs}:")
                appendLine("  Body       : ${sms.body}")
                if (sms.isOtp)              appendLine("  ⚠ OTP message detected")
                if (sms.extractedUpi != null) appendLine("  ⚠ UPI ID   : ${sms.extractedUpi}")
                if (sms.containsLink)       appendLine("  ⚠ Contains link")
            }
            appendLine()
        } else {
            appendLine("=== RECENT SMS / NOTIFICATIONS ===")
            appendLine("(none)")
            appendLine()
        }

        // --- Transcript ---
        if (conversationWindow.isNotEmpty()) {
            appendLine("=== CONVERSATION TRANSCRIPT (${conversationWindow.size} turns, oldest first) ===")
            conversationWindow.forEach { appendLine(it) }
            appendLine()
        } else {
            appendLine("=== CONVERSATION TRANSCRIPT ===")
            appendLine("(no transcript available — SMS/metadata only analysis)")
            appendLine()
        }

        // --- Acoustic features ---
        val af = ctx.acousticFeatures
        appendLine("=== ACOUSTIC FEATURES ===")
        appendLine("Caller talk ratio  : ${"%.0f".format(af.callerTalkRatio * 100)}%")
        appendLine("Caller WPM         : ${"%.0f".format(af.callerWordsPerMin)}")
        appendLine("User WPM           : ${"%.0f".format(af.userWordsPerMin)}")
        appendLine("Interruptions      : ${af.interruptionCount}")
        appendLine("Silence periods    : ${"%.1f".format(af.silencePeriodsSec)}s")
        af.avgPitchHz?.let { appendLine("Avg caller pitch   : ${"%.0f".format(it)} Hz") }
        appendLine()

        // --- Device context ---
        val dc = ctx.deviceContext
        appendLine("=== DEVICE CONTEXT ===")
        appendLine("Network    : ${dc.networkState}")
        if (dc.batteryPercent >= 0) appendLine("Battery    : ${dc.batteryPercent}%")
        appendLine("Screen on  : ${dc.isScreenOn}")
        dc.locationCity?.let { appendLine("City       : $it") }
        dc.activeAppPackage?.let { appendLine("Foreground : $it") }
        appendLine()

        appendLine("=== YOUR ANALYSIS ===")
    }

    /**
     * Parses the model's JSON response into a [ThreatScore].
     * Falls back to a neutral score if parsing fails.
     */
    private fun parseResponse(raw: String): ThreatScore {
        return try {
            val jsonStart = raw.indexOf('{')
            val jsonEnd   = raw.lastIndexOf('}')
            if (jsonStart == -1 || jsonEnd == -1) {
                throw IllegalArgumentException("No JSON object in response")
            }
            val json = raw.substring(jsonStart, jsonEnd + 1)

            val score = Regex(""""score"\s*:\s*([0-9.]+)""").find(json)
                ?.groupValues?.get(1)?.toFloatOrNull() ?: 0f

            val label = Regex(""""label"\s*:\s*"([^"]+)"""").find(json)
                ?.groupValues?.get(1) ?: "UNKNOWN"

            val confidence = Regex(""""confidence"\s*:\s*([0-9.]+)""").find(json)
                ?.groupValues?.get(1)?.toFloatOrNull() ?: 0f

            // Parse signals array: ["SIGNAL_A", "SIGNAL_B"]
            val signalsMatch = Regex(""""signals"\s*:\s*\[([^\]]*)]""").find(json)
            val signals = signalsMatch?.groupValues?.get(1)
                ?.split(",")
                ?.map { it.trim().removeSurrounding("\"") }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            val stageStr = Regex(""""stage"\s*:\s*"([^"]+)"""").find(json)
                ?.groupValues?.get(1) ?: "UNKNOWN"
            val stage = runCatching { ConversationStage.valueOf(stageStr) }
                .getOrDefault(ConversationStage.UNKNOWN)

            ThreatScore(
                score      = score.coerceIn(0f, 1f),
                label      = label,
                confidence = confidence.coerceIn(0f, 1f),
                signals    = signals,
                stage      = stage,
                rawOutput  = raw
            )
        } catch (e: Exception) {
            Log.w(TAG, "JSON parse failed: ${e.message} — raw=$raw")
            ThreatScore(score = 0.5f, label = "PARSE_ERROR", confidence = 0f, rawOutput = raw)
        }
    }
}
