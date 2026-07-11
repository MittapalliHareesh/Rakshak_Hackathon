package com.androidblunders.rakshak.core.model

/**
 * A single normalized inbound message from any [com.androidblunders.rakshak.core.contract.MessageSource]
 * (SMS receiver, notification listener, future WhatsApp accessibility hook, mock).
 *
 * The SMS Reader module produces these; the orchestrator lifts them into a
 * [CallContext] before analysis.
 */
data class MessagePayload(
    val sender: String,
    val body: String,
    val channel: CallContext.Channel,
    val packageName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)
