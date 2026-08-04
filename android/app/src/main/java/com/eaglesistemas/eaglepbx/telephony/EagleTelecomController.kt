package com.eaglesistemas.eaglepbx.telephony

import android.content.Context
import android.net.Uri
import android.telecom.DisconnectCause
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallControlResult
import androidx.core.telecom.CallControlScope
import androidx.core.telecom.CallsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Process-scoped bridge between Liblinphone and Android Telecom. */
class EagleTelecomController(
    context: Context,
    private val answerSipCall: () -> Boolean,
    private val disconnectSipCall: (DisconnectCause) -> Unit
) {
    private val callsManager = runCatching {
        CallsManager(context.applicationContext).apply {
            registerAppWithTelecom(CallsManager.CAPABILITY_BASELINE)
        }
    }.getOrNull()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()
    private var activeSession: TelecomCallSession? = null

    private sealed interface TelecomCommand {
        data object Answer : TelecomCommand
        data object SetActive : TelecomCommand
        data class Disconnect(val cause: DisconnectCause) : TelecomCommand
    }

    private data class TelecomCallSession(
        val key: String,
        val commands: Channel<TelecomCommand> = Channel(Channel.UNLIMITED)
    )

    fun registerIncoming(call: IncomingSipCall, callId: String) {
        val manager = callsManager ?: return
        val key = callId.ifBlank { call.number.filter(Char::isDigit) }
        val session = synchronized(lock) {
            if (activeSession != null) return
            TelecomCallSession(key).also { activeSession = it }
        }
        coroutineScope.launch {
            val attributes = CallAttributesCompat(
                displayName = call.displayName ?: call.number,
                address = Uri.parse("sip:${call.number}"),
                direction = CallAttributesCompat.DIRECTION_INCOMING,
                callType = CallAttributesCompat.CALL_TYPE_AUDIO_CALL,
                callCapabilities = 0
            )
            try {
                manager.addCall(
                    attributes,
                    onAnswer = {
                        val answered = withContext(Dispatchers.Main.immediate) {
                            answerSipCall()
                        }
                        check(answered) { "SIP controller unavailable" }
                    },
                    onDisconnect = { cause ->
                        withContext(Dispatchers.Main.immediate) {
                            disconnectSipCall(cause)
                        }
                        session.commands.close()
                    },
                    onSetActive = { Unit },
                    onSetInactive = { Unit }
                ) {
                    launch { processCommands(session) }
                }
            } catch (_: Exception) {
                // The custom CallStyle flow remains available if an OEM Telecom
                // implementation refuses a self-managed call.
            } finally {
                synchronized(lock) {
                    if (activeSession === session) activeSession = null
                }
                session.commands.close()
            }
        }
    }

    fun answerFromApp() {
        activeSession()?.commands?.trySend(TelecomCommand.Answer)
    }

    fun markActive() {
        activeSession()?.commands?.trySend(TelecomCommand.SetActive)
    }

    fun disconnect(cause: Int) {
        activeSession()?.commands?.trySend(
            TelecomCommand.Disconnect(DisconnectCause(cause))
        )
    }

    private fun activeSession(): TelecomCallSession? = synchronized(lock) {
        activeSession
    }

    private suspend fun CallControlScope.processCommands(session: TelecomCallSession) {
        session.commands.consumeEach { command ->
            when (command) {
                TelecomCommand.Answer -> {
                    when (answer(CallAttributesCompat.CALL_TYPE_AUDIO_CALL)) {
                        is CallControlResult.Error -> Unit
                        is CallControlResult.Success -> Unit
                    }
                }
                TelecomCommand.SetActive -> {
                    when (setActive()) {
                        is CallControlResult.Error -> Unit
                        is CallControlResult.Success -> Unit
                    }
                }
                is TelecomCommand.Disconnect -> {
                    disconnect(command.cause)
                    session.commands.close()
                }
            }
        }
    }
}
