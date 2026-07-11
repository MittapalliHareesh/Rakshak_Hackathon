package com.androidblunders.rakshak.core.contract

import com.androidblunders.rakshak.core.model.MessagePayload
import kotlinx.coroutines.flow.SharedFlow

/**
 * Plug-and-play contract for any inbound-message producer (SMS Reader module,
 * notification listener, future WhatsApp accessibility hook, mock feeder).
 *
 * The orchestrator merges the [incomingMessages] of every bound source, so the
 * downstream pipeline never cares where a message originated.
 */
interface MessageSource {
    val id: String
    val incomingMessages: SharedFlow<MessagePayload>
    fun startListening()
    fun stopListening()
}
