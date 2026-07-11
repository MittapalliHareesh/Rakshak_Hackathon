package com.androidblunders.rakshak.core.model

/**
 * Playback priority for [com.androidblunders.rakshak.core.contract.TextToSpeechEngine].
 * A [CRITICAL] barge-in must interrupt any lower-priority speech and grab audio focus.
 */
enum class Priority {
    LOW,
    NORMAL,
    CRITICAL,
}
