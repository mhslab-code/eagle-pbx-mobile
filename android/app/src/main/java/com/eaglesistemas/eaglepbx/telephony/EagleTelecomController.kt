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
    private var pendingIncoming: PendingTelecomCall? = null

    private sealed interface TelecomCommand {
        data object Answer : TelecomCommand
        data object SetActive : TelecomCommand
        data class Disconnect(val cause: DisconnectCause) : TelecomCommand
    }

    private class TelecomCallSession(
        val key: String,
        var sipCallId: String?,
        val commands: Channel<TelecomCommand> = Channel(Channel.UNLIMITED)
    ) {
        fun matches(otherKey: String, otherSipCallId: String?): Boolean = if (
            sipCallId != null && otherSipCallId != null
        ) {
            sipCallId == otherSipCallId
        } else {
            key == otherKey
        }
    }

    private data class PendingTelecomCall(
        val call: IncomingSipCall,
        val session: TelecomCallSession
    )

    fun registerIncoming(call: IncomingSipCall, callId: String) {
        val manager = callsManager ?: return
        val key = callId.ifBlank { call.number.filter(Char::isDigit) }
        val sipCallId = call.sipCallId?.takeIf(String::isNotBlank)
        val session = TelecomCallSession(key, sipCallId)
        val startNow = synchronized(lock) {
            val active = activeSession
            val sameAsActive = active?.matches(key, sipCallId) == true
            if (sameAsActive) {
                if (sipCallId != null) active?.sipCallId = sipCallId
                false
            } else if (active == null) {
                activeSession = session
                true
            } else {
                val pending = pendingIncoming
                if (pending?.session?.matches(key, sipCallId) == true) {
                    if (sipCallId != null) pending.session.sipCallId = sipCallId
                } else {
                    pending?.session?.commands?.close()
                    pendingIncoming = PendingTelecomCall(call, session)
                }
                false
            }
        }
        if (!startNow) return
        launchTelecomCall(manager, call, session)
    }

    private fun launchTelecomCall(
        manager: CallsManager,
        call: IncomingSipCall,
        session: TelecomCallSession
    ) {
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
                val next = synchronized(lock) {
                    if (activeSession === session) activeSession = null
                    pendingIncoming?.also {
                        pendingIncoming = null
                        activeSession = it.session
                    }
                }
                session.commands.close()
                if (next != null) {
                    launchTelecomCall(manager, next.call, next.session)
                }
            }
        }
    }

    fun bindSipCall(callId: String, sipCallId: String) {
        if (sipCallId.isBlank()) return
        synchronized(lock) {
            val sessions = listOfNotNull(
                activeSession,
                pendingIncoming?.session
            )
            sessions.firstOrNull { session ->
                session.key == callId ||
                    (callId.isBlank() && session.sipCallId == sipCallId)
            }?.sipCallId = sipCallId
        }
    }

    fun answerFromApp() {
        currentSession()?.commands?.trySend(TelecomCommand.Answer)
    }

    fun markActive() {
        currentSession()?.commands?.trySend(TelecomCommand.SetActive)
    }

    fun disconnect(cause: Int) {
        currentSession()?.commands?.trySend(
            TelecomCommand.Disconnect(DisconnectCause(cause))
        )
    }

    fun disconnectSipCall(sipCallId: String, cause: Int): Boolean {
        val session = synchronized(lock) {
            val pending = pendingIncoming
            if (pending?.session?.sipCallId == sipCallId) {
                pendingIncoming = null
                pending.session.commands.close()
                return true
            }
            activeSession?.takeIf { it.sipCallId == sipCallId }
        } ?: return false
        session.commands.trySend(
            TelecomCommand.Disconnect(DisconnectCause(cause))
        )
        return true
    }

    private fun currentSession(): TelecomCallSession? = synchronized(lock) {
        pendingIncoming?.session ?: activeSession
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
