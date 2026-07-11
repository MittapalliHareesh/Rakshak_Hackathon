package com.androidblunders.rakshak.detection

import com.androidblunders.rakshak.core.contract.MessageSource
import com.androidblunders.rakshak.core.model.CallContext
import com.androidblunders.rakshak.core.model.MessagePayload
import com.androidblunders.rakshak.messaging.MessageExtractor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Bridges the already-shipped notification listener ([MessageExtractor]) into the
 * generic [MessageSource] contract, so SMS/WhatsApp notifications flow straight
 * into the orchestrator. The dedicated SMS Reader module (BroadcastReceiver) can
 * be added later as a second [MessageSource] with no orchestrator change.
 */
@Singleton
class NotificationMessageSource @Inject constructor() : MessageSource {

    override val id: String = "notification-listener"

    private val _messages = MutableSharedFlow<MessagePayload>(extraBufferCapacity = 16)
    override val incomingMessages: SharedFlow<MessagePayload> = _messages.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob())
    private var job: Job? = null

    override fun startListening() {
        if (job != null) return
        job = scope.launch {
            MessageExtractor.messageFlow.collect { data ->
                _messages.tryEmit(
                    MessagePayload(
                        sender = data.sender,
                        body = data.content,
                        channel = CallContext.Channel.NOTIFICATION,
                        packageName = data.packageName,
                        timestamp = data.timestamp,
                    ),
                )
            }
        }
    }

    override fun stopListening() {
        job?.cancel()
        job = null
    }
}
