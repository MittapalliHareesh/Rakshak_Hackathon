package com.androidblunders.rakshak.orchestrator

import com.androidblunders.rakshak.core.model.CallContext
import com.androidblunders.rakshak.core.model.UserPreferences
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton holder of shared, cross-module context: the active session id, the
 * rolling conversation, and user preferences. Injected wherever synchronized
 * state is needed (analyzers, responders, UI), per the "Context Injection" design.
 */
@Singleton
class SessionManager @Inject constructor() {

    private val _preferences = MutableStateFlow(UserPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    private val _activeContext = MutableStateFlow<CallContext?>(null)
    val activeContext: StateFlow<CallContext?> = _activeContext.asStateFlow()

    fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        _preferences.update(transform)
    }

    /** Open a fresh analysis session for a channel + sender, discarding any prior turns. */
    fun startSession(channel: CallContext.Channel, sender: String): CallContext {
        val context = CallContext(
            sessionId = UUID.randomUUID().toString(),
            channel = channel,
            sender = sender,
        )
        _activeContext.value = context
        return context
    }

    /** Append a turn to the current session (creating one if none is active). */
    fun appendTurn(turn: CallContext.Turn): CallContext {
        val current = _activeContext.value
            ?: startSession(CallContext.Channel.UNKNOWN, sender = "")
        val updated = current.copy(turns = (current.turns + turn).takeLast(MAX_TURNS))
        _activeContext.value = updated
        return updated
    }

    fun endSession() {
        _activeContext.value = null
    }

    private companion object {
        /** Rolling window kept in memory, matching the Gemma context-window design. */
        const val MAX_TURNS = 10
    }
}
