package com.eaglesistemas.eaglepbx.telephony

import android.content.Context
import com.eaglesistemas.eaglepbx.data.SipProvisioning
import org.linphone.core.Account
import org.linphone.core.AudioDevice
import org.linphone.core.AuthInfo
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.MediaEncryption
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType

enum class SipEngineStatus {
    INITIALIZING,
    READY,
    REGISTERING,
    REGISTERED,
    REGISTRATION_FAILED,
    UNAVAILABLE
}

enum class SipCallStatus {
    IDLE,
    INCOMING,
    OUTGOING,
    RINGING,
    CONNECTED,
    HELD,
    ENDING,
    FAILED
}

enum class AttendedTransferStatus {
    IDLE,
    CALLING,
    CONNECTED,
    COMPLETING,
    FAILED
}

enum class ConferenceSetupStatus {
    IDLE,
    CALLING,
    CONNECTED,
    JOINING,
    ACTIVE,
    FAILED
}

data class IncomingSipCall(
    val number: String,
    val displayName: String?
)

data class SipAudioOutput(
    val id: String,
    val label: String,
    val selected: Boolean
)

/**
 * Owns the native SIP core and the authenticated per-device account.
 *
 * SIP credentials are supplied only by the provisioning flow, remain in
 * memory, and must never be embedded or logged here.
 */
class LinphoneEngine(
    context: Context,
    private val onStatusChanged: (SipEngineStatus) -> Unit,
    private val onCallStatusChanged: (SipCallStatus) -> Unit,
    private val onIncomingCallChanged: (IncomingSipCall?) -> Unit,
    private val onAttendedTransferChanged: (AttendedTransferStatus) -> Unit,
    private val onConferenceSetupChanged: (ConferenceSetupStatus) -> Unit
) {
    private val core: Core = Factory.instance().createCore(
        null,
        null,
        context.applicationContext
    )
    private var account: Account? = null
    private var authInfo: AuthInfo? = null
    private var activeCall: Call? = null
    private var originalTransferCall: Call? = null
    private var consultationCall: Call? = null
    private var releasedConsultationCall: Call? = null
    private var cancellingConsultation = false
    private var originalConferenceCall: Call? = null
    private var addedConferenceCall: Call? = null
    private var releasedConferenceCall: Call? = null
    private var cancellingAddedCall = false
    private val conferenceCalls = mutableSetOf<Call>()
    private var sipDomain: String? = null
    private val listener = object : CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState,
            message: String
        ) {
            onStatusChanged(
                when (state) {
                    RegistrationState.Ok -> SipEngineStatus.REGISTERED
                    RegistrationState.Progress,
                    RegistrationState.Refreshing -> SipEngineStatus.REGISTERING
                    RegistrationState.Failed -> SipEngineStatus.REGISTRATION_FAILED
                    RegistrationState.Cleared,
                    RegistrationState.None -> SipEngineStatus.READY
                }
            )
        }

        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State,
            message: String
        ) {
            if (call in conferenceCalls) {
                when (state) {
                    Call.State.Connected,
                    Call.State.StreamsRunning -> {
                        activeCall = call
                        onCallStatusChanged(SipCallStatus.CONNECTED)
                    }
                    Call.State.Error,
                    Call.State.End,
                    Call.State.Released -> {
                        conferenceCalls.remove(call)
                        if (conferenceCalls.isEmpty()) {
                            activeCall = null
                            onConferenceSetupChanged(ConferenceSetupStatus.IDLE)
                            onCallStatusChanged(SipCallStatus.IDLE)
                        } else {
                            activeCall = conferenceCalls.first()
                            onCallStatusChanged(SipCallStatus.CONNECTED)
                        }
                    }
                    else -> Unit
                }
                return
            }
            if (call == releasedConferenceCall) {
                if (state == Call.State.Released) releasedConferenceCall = null
                return
            }
            if (call == addedConferenceCall) {
                when (state) {
                    Call.State.OutgoingInit,
                    Call.State.OutgoingProgress,
                    Call.State.OutgoingRinging,
                    Call.State.OutgoingEarlyMedia -> {
                        activeCall = call
                        onConferenceSetupChanged(ConferenceSetupStatus.CALLING)
                    }
                    Call.State.Connected -> Unit
                    Call.State.StreamsRunning -> {
                        activeCall = call
                        onCallStatusChanged(SipCallStatus.CONNECTED)
                        onConferenceSetupChanged(ConferenceSetupStatus.CONNECTED)
                    }
                    Call.State.Error,
                    Call.State.End,
                    Call.State.Released -> {
                        if (addedConferenceCall == call) {
                            releasedConferenceCall = call
                            addedConferenceCall = null
                            val original = originalConferenceCall
                            originalConferenceCall = null
                            activeCall = original
                            if (original != null) {
                                original.resume()
                                onCallStatusChanged(SipCallStatus.CONNECTED)
                            }
                            if (state == Call.State.Error && !cancellingAddedCall) {
                                onConferenceSetupChanged(ConferenceSetupStatus.FAILED)
                            } else {
                                onConferenceSetupChanged(ConferenceSetupStatus.IDLE)
                            }
                            cancellingAddedCall = false
                        }
                    }
                    else -> Unit
                }
                return
            }
            if (call == releasedConsultationCall) {
                if (state == Call.State.Released) releasedConsultationCall = null
                return
            }
            if (call == consultationCall) {
                when (state) {
                    Call.State.OutgoingInit,
                    Call.State.OutgoingProgress,
                    Call.State.OutgoingRinging,
                    Call.State.OutgoingEarlyMedia -> {
                        activeCall = call
                        onAttendedTransferChanged(AttendedTransferStatus.CALLING)
                    }
                    Call.State.Connected -> Unit
                    Call.State.StreamsRunning -> {
                        activeCall = call
                        onCallStatusChanged(SipCallStatus.CONNECTED)
                        onAttendedTransferChanged(AttendedTransferStatus.CONNECTED)
                    }
                    Call.State.Error,
                    Call.State.End,
                    Call.State.Released -> {
                        if (consultationCall == call) {
                            releasedConsultationCall = call
                            consultationCall = null
                            val original = originalTransferCall
                            originalTransferCall = null
                            activeCall = original
                            if (original != null) {
                                original.resume()
                                onCallStatusChanged(SipCallStatus.CONNECTED)
                            }
                            if (state == Call.State.Error && !cancellingConsultation) {
                                onAttendedTransferChanged(AttendedTransferStatus.FAILED)
                            } else {
                                onAttendedTransferChanged(AttendedTransferStatus.IDLE)
                            }
                            cancellingConsultation = false
                        }
                    }
                    else -> Unit
                }
                return
            }
            when (state) {
                Call.State.IncomingReceived,
                Call.State.IncomingEarlyMedia -> {
                    activeCall = call
                    val remote = call.remoteAddress
                    onIncomingCallChanged(
                        IncomingSipCall(
                            number = remote.username.orEmpty(),
                            displayName = remote.displayName
                                ?.trim()
                                ?.takeIf(String::isNotBlank)
                        )
                    )
                    onCallStatusChanged(SipCallStatus.INCOMING)
                }
                Call.State.OutgoingInit,
                Call.State.OutgoingProgress -> {
                    activeCall = call
                    onCallStatusChanged(SipCallStatus.OUTGOING)
                }
                Call.State.OutgoingRinging,
                Call.State.OutgoingEarlyMedia -> {
                    activeCall = call
                    onCallStatusChanged(SipCallStatus.RINGING)
                }
                Call.State.Connected,
                Call.State.StreamsRunning -> {
                    activeCall = call
                    onIncomingCallChanged(null)
                    onCallStatusChanged(SipCallStatus.CONNECTED)
                }
                Call.State.Pausing,
                Call.State.Paused,
                Call.State.PausedByRemote -> {
                    activeCall = call
                    onCallStatusChanged(SipCallStatus.HELD)
                }
                Call.State.Resuming -> {
                    activeCall = call
                    onCallStatusChanged(SipCallStatus.CONNECTED)
                }
                Call.State.Error -> {
                    activeCall = null
                    onIncomingCallChanged(null)
                    onCallStatusChanged(SipCallStatus.FAILED)
                }
                Call.State.End,
                Call.State.Released -> {
                    activeCall = null
                    onIncomingCallChanged(null)
                    onCallStatusChanged(SipCallStatus.IDLE)
                }
                else -> Unit
            }
        }
    }

    fun start() {
        core.addListener(listener)
        core.start()
    }

    fun configure(provisioning: SipProvisioning) {
        require(provisioning.transport == "tls")
        clearAccount()
        val factory = Factory.instance()
        val identity = requireNotNull(
            factory.createAddress(
                "sip:${provisioning.username}@${provisioning.domain}"
            )
        )
        val server = requireNotNull(
            factory.createAddress(
                "sips:${provisioning.domain}:${provisioning.port};transport=tls"
            )
        )
        val credentials = factory.createAuthInfo(
            provisioning.username,
            null,
            provisioning.password,
            null,
            null,
            provisioning.domain
        )
        val params = core.createAccountParams().apply {
            identityAddress = identity
            serverAddress = server
            transport = TransportType.Tls
            isRegisterEnabled = true
            expires = 600
        }
        core.mediaEncryption = MediaEncryption.SRTP
        core.isMediaEncryptionMandatory = true
        val configuredAccount = core.createAccount(params)
        core.addAuthInfo(credentials)
        core.addAccount(configuredAccount)
        core.defaultAccount = configuredAccount
        authInfo = credentials
        account = configuredAccount
        sipDomain = provisioning.domain
        onStatusChanged(SipEngineStatus.REGISTERING)
    }

    fun placeCall(destination: String): Boolean {
        val normalized = destination.trim()
        if (
            activeCall != null ||
            normalized.isBlank() ||
            !normalized.matches(Regex("[0-9*#+]{1,40}"))
        ) return false
        val domain = sipDomain ?: return false
        val address = Factory.instance().createAddress("sip:$normalized@$domain")
            ?: return false
        return runCatching {
            activeCall = core.inviteAddress(address)
            activeCall != null
        }.getOrDefault(false)
    }

    fun hangupCall() {
        if (conferenceCalls.isNotEmpty()) {
            onCallStatusChanged(SipCallStatus.ENDING)
            conferenceCalls.toList().forEach(Call::terminate)
            return
        }
        activeCall?.let {
            onCallStatusChanged(SipCallStatus.ENDING)
            it.terminate()
        }
    }

    fun sendDtmf(digit: Char): Boolean {
        if (digit !in "0123456789*#") return false
        val call = activeCall ?: return false
        if (call.state !in setOf(Call.State.Connected, Call.State.StreamsRunning)) {
            return false
        }
        return call.sendDtmf(digit) == 0
    }

    fun setMicrophoneMuted(muted: Boolean): Boolean {
        val call = activeCall ?: return false
        if (call.state !in setOf(Call.State.Connected, Call.State.StreamsRunning)) {
            return false
        }
        call.microphoneMuted = muted
        return call.microphoneMuted == muted
    }

    fun audioOutputs(): List<SipAudioOutput> {
        val selectedId = activeCall?.outputAudioDevice?.id
        return core.audioDevices
            .filter { it.hasCapability(AudioDevice.Capabilities.CapabilityPlay) }
            .distinctBy(AudioDevice::getId)
            .map { device ->
                SipAudioOutput(
                    id = device.id,
                    label = audioOutputLabel(device),
                    selected = device.id == selectedId
                )
            }
    }

    fun selectAudioOutput(id: String): Boolean {
        val call = activeCall ?: return false
        if (call.state !in setOf(Call.State.Connected, Call.State.StreamsRunning)) {
            return false
        }
        val device = core.audioDevices.firstOrNull {
            it.id == id && it.hasCapability(AudioDevice.Capabilities.CapabilityPlay)
        } ?: return false
        call.outputAudioDevice = device
        return call.outputAudioDevice?.id == id
    }

    fun setCallHeld(held: Boolean): Boolean {
        val call = activeCall ?: return false
        return if (held) {
            if (call.state !in setOf(Call.State.Connected, Call.State.StreamsRunning)) {
                false
            } else {
                call.pause() == 0
            }
        } else {
            if (call.state !in setOf(Call.State.Paused, Call.State.PausedByRemote)) {
                false
            } else {
                call.resume() == 0
            }
        }
    }

    fun transferDirect(destination: String): Boolean {
        val normalized = destination.trim()
        val call = activeCall ?: return false
        if (
            call.state !in setOf(Call.State.Connected, Call.State.StreamsRunning) ||
            normalized.isBlank() ||
            !normalized.matches(Regex("[0-9*#+]{1,40}"))
        ) return false
        val domain = sipDomain ?: return false
        val address = Factory.instance().createAddress("sip:$normalized@$domain")
            ?: return false
        val accepted = call.transferTo(address) == 0
        if (accepted) onCallStatusChanged(SipCallStatus.ENDING)
        return accepted
    }

    fun startAttendedTransfer(destination: String): Boolean {
        val normalized = destination.trim()
        val original = activeCall ?: return false
        if (
            originalTransferCall != null ||
            original.state !in setOf(Call.State.Connected, Call.State.StreamsRunning) ||
            normalized.isBlank() ||
            !normalized.matches(Regex("[0-9*#+]{1,40}"))
        ) return false
        val domain = sipDomain ?: return false
        val consultationDestination = noVoicemailConsultationDestination(normalized)
        val address = Factory.instance().createAddress("sip:$consultationDestination@$domain")
            ?: return false
        return runCatching {
            if (original.pause() != 0) return@runCatching false
            originalTransferCall = original
            val consultation = core.inviteAddress(address)
            if (consultation == null) {
                originalTransferCall = null
                original.resume()
                return@runCatching false
            }
            consultationCall = consultation
            activeCall = consultation
            onAttendedTransferChanged(AttendedTransferStatus.CALLING)
            true
        }.getOrDefault(false)
    }

    fun cancelAttendedTransfer(): Boolean {
        val original = originalTransferCall ?: return false
        val consultation = consultationCall
        cancellingConsultation = true
        consultation?.terminate()
        if (consultation == null) {
            originalTransferCall = null
            activeCall = original
            cancellingConsultation = false
            val accepted = original.resume() == 0
            if (accepted) onCallStatusChanged(SipCallStatus.CONNECTED)
            onAttendedTransferChanged(AttendedTransferStatus.IDLE)
            return accepted
        }
        return true
    }

    fun completeAttendedTransfer(): Boolean {
        val original = originalTransferCall ?: return false
        val consultation = consultationCall ?: return false
        if (
            consultation.state !in setOf(Call.State.Connected, Call.State.StreamsRunning)
        ) return false
        onAttendedTransferChanged(AttendedTransferStatus.COMPLETING)
        val accepted = original.transferToAnother(consultation) == 0
        if (!accepted) {
            onAttendedTransferChanged(AttendedTransferStatus.CONNECTED)
        } else {
            onCallStatusChanged(SipCallStatus.ENDING)
        }
        return accepted
    }

    fun startAdditionalCall(destination: String): Boolean {
        val normalized = destination.trim()
        val original = activeCall ?: return false
        if (
            originalConferenceCall != null ||
            originalTransferCall != null ||
            original.state !in setOf(Call.State.Connected, Call.State.StreamsRunning) ||
            normalized.isBlank() ||
            !normalized.matches(Regex("[0-9*#+]{1,40}"))
        ) return false
        val domain = sipDomain ?: return false
        val consultationDestination = noVoicemailConsultationDestination(normalized)
        val address = Factory.instance().createAddress("sip:$consultationDestination@$domain")
            ?: return false
        return runCatching {
            if (original.pause() != 0) return@runCatching false
            originalConferenceCall = original
            val additional = core.inviteAddress(address)
            if (additional == null) {
                originalConferenceCall = null
                original.resume()
                return@runCatching false
            }
            addedConferenceCall = additional
            activeCall = additional
            onConferenceSetupChanged(ConferenceSetupStatus.CALLING)
            true
        }.getOrDefault(false)
    }

    private fun noVoicemailConsultationDestination(destination: String): String {
        return if (destination.matches(Regex("10[1-5]"))) {
            "88$destination"
        } else {
            destination
        }
    }

    fun cancelAdditionalCall(): Boolean {
        val original = originalConferenceCall ?: return false
        val additional = addedConferenceCall
        cancellingAddedCall = true
        additional?.terminate()
        if (additional == null) {
            originalConferenceCall = null
            activeCall = original
            cancellingAddedCall = false
            val accepted = original.resume() == 0
            if (accepted) onCallStatusChanged(SipCallStatus.CONNECTED)
            onConferenceSetupChanged(ConferenceSetupStatus.IDLE)
            return accepted
        }
        return true
    }

    fun completeConference(): Boolean {
        val original = originalConferenceCall ?: return false
        val additional = addedConferenceCall ?: return false
        if (
            additional.state !in setOf(Call.State.Connected, Call.State.StreamsRunning)
        ) return false
        onConferenceSetupChanged(ConferenceSetupStatus.JOINING)
        val accepted = core.addAllToConference() == 0
        if (accepted) {
            conferenceCalls.add(original)
            conferenceCalls.add(additional)
            originalConferenceCall = null
            addedConferenceCall = null
            activeCall = additional
            onCallStatusChanged(SipCallStatus.CONNECTED)
            onConferenceSetupChanged(ConferenceSetupStatus.ACTIVE)
        } else {
            onConferenceSetupChanged(ConferenceSetupStatus.CONNECTED)
        }
        return accepted
    }

    private fun audioOutputLabel(device: AudioDevice): String {
        val type = when (device.type) {
            AudioDevice.Type.Earpiece -> "Auricular"
            AudioDevice.Type.Speaker -> "Alto-falante"
            AudioDevice.Type.Bluetooth,
            AudioDevice.Type.BluetoothA2DP -> "Bluetooth"
            AudioDevice.Type.Headset -> "Headset"
            AudioDevice.Type.Headphones -> "Fones de ouvido"
            AudioDevice.Type.HearingAid -> "Aparelho auditivo"
            AudioDevice.Type.Hdmi -> "HDMI"
            AudioDevice.Type.GenericUsb -> "Áudio USB"
            AudioDevice.Type.AuxLine -> "Saída auxiliar"
            else -> "Saída de áudio"
        }
        val name = device.deviceName.trim()
        return if (name.isBlank() || name.equals(type, ignoreCase = true)) {
            type
        } else {
            "$type · $name"
        }
    }

    fun acceptIncomingCall(): Boolean {
        val call = activeCall ?: return false
        return call.accept() == 0
    }

    fun rejectIncomingCall(): Boolean {
        val call = activeCall ?: return false
        val accepted = call.terminate() == 0
        if (accepted) {
            onIncomingCallChanged(null)
            onCallStatusChanged(SipCallStatus.ENDING)
        }
        return accepted
    }

    fun clearAccount() {
        consultationCall?.terminate()
        originalTransferCall?.terminate()
        addedConferenceCall?.terminate()
        originalConferenceCall?.terminate()
        conferenceCalls.toList().forEach(Call::terminate)
        conferenceCalls.clear()
        consultationCall = null
        originalTransferCall = null
        releasedConsultationCall = null
        cancellingConsultation = false
        addedConferenceCall = null
        originalConferenceCall = null
        releasedConferenceCall = null
        cancellingAddedCall = false
        activeCall?.terminate()
        activeCall = null
        account?.let { core.removeAccount(it) }
        authInfo?.let { core.removeAuthInfo(it) }
        account = null
        authInfo = null
        sipDomain = null
        onIncomingCallChanged(null)
        onAttendedTransferChanged(AttendedTransferStatus.IDLE)
        onConferenceSetupChanged(ConferenceSetupStatus.IDLE)
        onCallStatusChanged(SipCallStatus.IDLE)
    }

    fun stop() {
        clearAccount()
        core.removeListener(listener)
        core.stop()
    }
}
