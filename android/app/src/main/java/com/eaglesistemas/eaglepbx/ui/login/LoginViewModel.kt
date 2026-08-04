package com.eaglesistemas.eaglepbx.ui.login

import android.app.Application
import android.telecom.DisconnectCause
import android.media.MediaPlayer
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eaglesistemas.eaglepbx.data.ApiException
import com.eaglesistemas.eaglepbx.data.AuthenticatedUser
import com.eaglesistemas.eaglepbx.data.EagleApiClient
import com.eaglesistemas.eaglepbx.data.EagleContact
import com.eaglesistemas.eaglepbx.data.HistoryCall
import com.eaglesistemas.eaglepbx.data.DeviceIdentityStore
import com.eaglesistemas.eaglepbx.data.MobileDeviceRegistration
import com.eaglesistemas.eaglepbx.data.SecureSessionStore
import com.google.firebase.messaging.FirebaseMessaging
import com.eaglesistemas.eaglepbx.telephony.LinphoneEngine
import com.eaglesistemas.eaglepbx.telephony.IncomingSipCall
import com.eaglesistemas.eaglepbx.telephony.SipCallStatus
import com.eaglesistemas.eaglepbx.EaglePbxApplication
import com.eaglesistemas.eaglepbx.telephony.SipEngineStatus
import com.eaglesistemas.eaglepbx.telephony.SipAudioOutput
import com.eaglesistemas.eaglepbx.telephony.SipForegroundService
import com.eaglesistemas.eaglepbx.telephony.AttendedTransferStatus
import com.eaglesistemas.eaglepbx.telephony.ConferenceSetupStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

data class LoginUiState(
    val restoringSession: Boolean = true,
    val submitting: Boolean = false,
    val updatingPresence: Boolean = false,
    val loadingContacts: Boolean = false,
    val contactsLoaded: Boolean = false,
    val contacts: List<EagleContact> = emptyList(),
    val loadingHistory: Boolean = false,
    val historyLoaded: Boolean = false,
    val history: List<HistoryCall> = emptyList(),
    val loadingRecordingId: String? = null,
    val activeRecordingId: String? = null,
    val recordingPlaying: Boolean = false,
    val recordingPosition: Int = 0,
    val recordingDuration: Int = 0,
    val savingProfile: Boolean = false,
    val profileMessage: String? = null,
    val profileError: String? = null,
    val sipEngineStatus: SipEngineStatus = SipEngineStatus.INITIALIZING,
    val sipCallStatus: SipCallStatus = SipCallStatus.IDLE,
    val attendedTransferStatus: AttendedTransferStatus = AttendedTransferStatus.IDLE,
    val conferenceSetupStatus: ConferenceSetupStatus = ConferenceSetupStatus.IDLE,
    val microphoneMuted: Boolean = false,
    val audioOutputs: List<SipAudioOutput> = emptyList(),
    val incomingSipCall: IncomingSipCall? = null,
    val sipCallError: String? = null,
    val registeringMobileDevice: Boolean = false,
    val mobileDevice: MobileDeviceRegistration? = null,
    val mobileDeviceError: String? = null,
    val user: AuthenticatedUser? = null,
    val error: String? = null,
    val connectionError: String? = null,
    val presenceError: String? = null,
    val contactsError: String? = null,
    val historyError: String? = null,
    val recordingError: String? = null
)

internal fun canPresentIncomingFromNotification(
    callStatus: SipCallStatus,
    serviceIncomingCallActive: Boolean
): Boolean = serviceIncomingCallActive && callStatus in setOf(
    SipCallStatus.IDLE,
    SipCallStatus.INCOMING,
    SipCallStatus.FAILED
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val api = EagleApiClient(
        SecureSessionStore(application),
        DeviceIdentityStore(application)
    )
    private var linphoneEngine: LinphoneEngine? = null
    private var sipIncomingWasActive = false
    private var notificationIncomingCall: IncomingSipCall? = null
    private var answerIncomingWhenReady = false
    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null
    private var contactsRequestInFlight = false
    private var historyRequestInFlight = false
    private val mutableState = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = mutableState.asStateFlow()

    private fun telecomController() =
        (getApplication<Application>() as? EaglePbxApplication)?.telecomController

    init {
        SipForegroundService.setAnswerCallHandler(::answerIncomingFromLockScreen)
        SipForegroundService.setRejectCallHandler(::rejectIncomingCall)
        SipForegroundService.setHangupCallHandler(::hangupCall)
        SipForegroundService.setIncomingNotificationHandler(
            ::onIncomingNotificationChanged
        )
        api.cachedUser()?.let { cachedUser ->
            mutableState.value = mutableState.value.copy(
                restoringSession = false,
                user = cachedUser
            )
            hydrateCachedData(cachedUser)
        }
        initializeSipEngine()
        viewModelScope.launch { restoreSessionWithRetry() }
    }

    private suspend fun restoreSessionWithRetry() {
        val cachedUser = withContext(Dispatchers.IO) { api.cachedUser() }
        while (true) {
            val result = runCatching {
                withContext(Dispatchers.IO) { api.restoreSession() }
            }
            val restoredUser = result.getOrNull()
            if (restoredUser != null) {
                mutableState.value = mutableState.value.copy(
                    restoringSession = false,
                    user = restoredUser,
                    error = null,
                    connectionError = null
                )
                hydrateAndRefreshCachedData(restoredUser)
                registerMobileDevice()
                return
            }
            val failure = result.exceptionOrNull()
            if (failure is IOException && cachedUser != null) {
                mutableState.value = mutableState.value.copy(
                    restoringSession = false,
                    user = cachedUser,
                    error = null,
                    connectionError = "Sem conexão"
                )
                hydrateCachedData(cachedUser)
                delay(3_000L)
                continue
            }
            mutableState.value = mutableState.value.copy(
                restoringSession = false,
                user = null,
                error = failure?.toFriendlyMessage(),
                connectionError = null
            )
            return
        }
    }

    private fun registerMobileDevice() {
        if (
            mutableState.value.user == null ||
            mutableState.value.registeringMobileDevice
        ) return
        mutableState.value = mutableState.value.copy(
            registeringMobileDevice = true,
            mobileDeviceError = null
        )
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    api.registerMobileDevice(
                        Build.MODEL.trim().ifBlank { "Dispositivo Android" }
                    )
                }
            }
            mutableState.value = mutableState.value.copy(
                registeringMobileDevice = false,
                mobileDevice = result.getOrNull(),
                mobileDeviceError = result.exceptionOrNull()?.toFriendlyMessage()
            )
            result.getOrNull()?.let {
                synchronizePushRegistration()
                if (it.status == "ready") configureSipAccount()
            }
        }
    }

    private fun synchronizePushRegistration() {
        if (mutableState.value.user == null) return
        FirebaseMessaging.getInstance().register()
    }

    private fun configureSipAccount() {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { api.mobileSipConfig() }
            }
            result.onSuccess { provisioning ->
                runCatching {
                    linphoneEngine?.configure(provisioning)
                        ?: error("Motor SIP indisponível")
                }.onFailure {
                    mutableState.value = mutableState.value.copy(
                        sipEngineStatus = SipEngineStatus.REGISTRATION_FAILED
                    )
                }
            }.onFailure {
                mutableState.value = mutableState.value.copy(
                    sipEngineStatus = SipEngineStatus.REGISTRATION_FAILED
                )
            }
        }
    }

    private fun initializeSipEngine() {
        runCatching {
            LinphoneEngine(
                context = getApplication(),
                onStatusChanged = { status ->
                    mutableState.value = mutableState.value.copy(
                        sipEngineStatus = status
                    )
                    if (status == SipEngineStatus.REGISTERED) {
                        SipForegroundService.start(getApplication())
                    }
                },
                onCallStatusChanged = { status ->
                    if (status == SipCallStatus.CONNECTED) {
                        answerIncomingWhenReady = false
                        notificationIncomingCall = null
                        SipForegroundService.markAnswered(getApplication())
                        telecomController()?.markActive()
                    } else if (status in setOf(SipCallStatus.IDLE, SipCallStatus.FAILED)) {
                        SipForegroundService.cancelIncoming(getApplication())
                        SipForegroundService.finishOngoingCall(getApplication())
                        telecomController()?.disconnect(DisconnectCause.REMOTE)
                    }
                    mutableState.value = mutableState.value.copy(
                        sipCallStatus = status,
                        incomingSipCall = if (status == SipCallStatus.CONNECTED) {
                            null
                        } else {
                            mutableState.value.incomingSipCall
                        },
                        microphoneMuted = if (status == SipCallStatus.IDLE) {
                            false
                        } else {
                            mutableState.value.microphoneMuted
                        },
                        audioOutputs = if (status == SipCallStatus.IDLE) {
                            emptyList()
                        } else {
                            mutableState.value.audioOutputs
                        }
                    )
                },
                onIncomingCallChanged = { call ->
                    mutableState.value = mutableState.value.copy(
                        incomingSipCall = call ?: notificationIncomingCall
                    )
                    if (call == null) {
                        sipIncomingWasActive = false
                    } else {
                        sipIncomingWasActive = true
                        notificationIncomingCall = call
                        SipForegroundService.showIncoming(getApplication(), call)
                        if (answerIncomingWhenReady) {
                            answerIncomingWhenReady = false
                            if (linphoneEngine?.acceptIncomingCall() == true) {
                                SipForegroundService.prepareForAnswer()
                            } else {
                                answerIncomingWhenReady = true
                            }
                        }
                    }
                    if (call != null && !mutableState.value.contactsLoaded) {
                        loadContacts(false)
                    }
                },
                onAttendedTransferChanged = { status ->
                    mutableState.value = mutableState.value.copy(
                        attendedTransferStatus = status
                    )
                },
                onConferenceSetupChanged = { status ->
                    mutableState.value = mutableState.value.copy(
                        conferenceSetupStatus = status
                    )
                }
            ).also {
                it.start()
                linphoneEngine = it
            }
        }.onSuccess {
            mutableState.value = mutableState.value.copy(
                sipEngineStatus = SipEngineStatus.READY
            )
        }.onFailure {
            mutableState.value = mutableState.value.copy(
                sipEngineStatus = SipEngineStatus.UNAVAILABLE
            )
        }
    }

    fun login(extension: String, password: String) {
        if (extension.isBlank() || password.isBlank() || mutableState.value.submitting) return
        mutableState.value = mutableState.value.copy(submitting = true, error = null)
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { api.login(extension.trim(), password) }
            }
            mutableState.value = mutableState.value.copy(
                submitting = false,
                user = result.getOrNull(),
                error = result.exceptionOrNull()?.toFriendlyMessage()
            )
            result.getOrNull()?.let { user ->
                hydrateAndRefreshCachedData(user)
                registerMobileDevice()
            }
        }
    }

    fun setApplicationInBackground(background: Boolean) {
        if (background) {
            linphoneEngine?.enterBackground()
        } else {
            linphoneEngine?.enterForeground()
        }
    }

    fun placeCall(destination: String) {
        if (
            mutableState.value.sipEngineStatus != SipEngineStatus.REGISTERED ||
            mutableState.value.sipCallStatus != SipCallStatus.IDLE
        ) return
        mutableState.value = mutableState.value.copy(
            sipCallStatus = SipCallStatus.OUTGOING
        )
        viewModelScope.launch(Dispatchers.IO) {
            if (linphoneEngine?.placeCall(destination) != true) {
                mutableState.value = mutableState.value.copy(
                    sipCallStatus = SipCallStatus.FAILED
                )
            }
        }
    }

    fun hangupCall() {
        linphoneEngine?.hangupCall()
    }

    fun sendDtmf(digit: Char) {
        if (mutableState.value.sipCallStatus != SipCallStatus.CONNECTED) return
        linphoneEngine?.sendDtmf(digit)
    }

    fun toggleMicrophone() {
        if (mutableState.value.sipCallStatus != SipCallStatus.CONNECTED) return
        val muted = !mutableState.value.microphoneMuted
        if (linphoneEngine?.setMicrophoneMuted(muted) == true) {
            mutableState.value = mutableState.value.copy(microphoneMuted = muted)
        }
    }

    fun loadAudioOutputs() {
        if (mutableState.value.sipCallStatus != SipCallStatus.CONNECTED) return
        mutableState.value = mutableState.value.copy(
            audioOutputs = linphoneEngine?.audioOutputs().orEmpty()
        )
    }

    fun selectAudioOutput(id: String) {
        if (mutableState.value.sipCallStatus != SipCallStatus.CONNECTED) return
        if (linphoneEngine?.selectAudioOutput(id) == true) {
            mutableState.value = mutableState.value.copy(
                audioOutputs = linphoneEngine?.audioOutputs().orEmpty()
            )
        }
    }

    fun toggleCallHold() {
        val current = mutableState.value.sipCallStatus
        if (current !in setOf(SipCallStatus.CONNECTED, SipCallStatus.HELD)) return
        linphoneEngine?.setCallHeld(current == SipCallStatus.CONNECTED)
    }

    fun transferDirect(destination: String): Boolean {
        if (mutableState.value.sipCallStatus != SipCallStatus.CONNECTED) return false
        return linphoneEngine?.transferDirect(destination) == true
    }

    fun startAttendedTransfer(destination: String): Boolean {
        if (mutableState.value.sipCallStatus != SipCallStatus.CONNECTED) return false
        mutableState.value = mutableState.value.copy(
            attendedTransferStatus = AttendedTransferStatus.CALLING
        )
        viewModelScope.launch(Dispatchers.IO) {
            if (linphoneEngine?.startAttendedTransfer(destination) != true) {
                mutableState.value = mutableState.value.copy(
                    attendedTransferStatus = AttendedTransferStatus.FAILED
                )
            }
        }
        return true
    }

    fun cancelAttendedTransfer(): Boolean =
        linphoneEngine?.cancelAttendedTransfer() == true

    fun completeAttendedTransfer(): Boolean =
        linphoneEngine?.completeAttendedTransfer() == true

    fun startAdditionalCall(destination: String): Boolean {
        if (mutableState.value.sipCallStatus != SipCallStatus.CONNECTED) return false
        mutableState.value = mutableState.value.copy(
            conferenceSetupStatus = ConferenceSetupStatus.CALLING
        )
        viewModelScope.launch(Dispatchers.IO) {
            if (linphoneEngine?.startAdditionalCall(destination) != true) {
                mutableState.value = mutableState.value.copy(
                    conferenceSetupStatus = ConferenceSetupStatus.FAILED
                )
            }
        }
        return true
    }

    fun cancelAdditionalCall(): Boolean =
        linphoneEngine?.cancelAdditionalCall() == true

    fun completeConference(): Boolean {
        if (
            mutableState.value.conferenceSetupStatus !=
            ConferenceSetupStatus.CONNECTED
        ) return false
        mutableState.value = mutableState.value.copy(
            conferenceSetupStatus = ConferenceSetupStatus.JOINING
        )
        viewModelScope.launch(Dispatchers.IO) {
            if (linphoneEngine?.completeConference() != true) {
                mutableState.value = mutableState.value.copy(
                    conferenceSetupStatus = ConferenceSetupStatus.CONNECTED
                )
            }
        }
        return true
    }

    fun acceptIncomingCall() {
        if (mutableState.value.sipCallStatus != SipCallStatus.INCOMING) return
        if (linphoneEngine?.acceptIncomingCall() == true) {
            SipForegroundService.prepareForAnswer()
            telecomController()?.answerFromApp()
        } else {
            mutableState.value = mutableState.value.copy(
                sipCallStatus = SipCallStatus.FAILED,
                incomingSipCall = null
            )
        }
    }

    fun presentIncomingFromNotification(call: IncomingSipCall?): Boolean {
        val serviceCall = SipForegroundService.currentIncomingCall() ?: return false
        if (!canPresentIncomingFromNotification(
                callStatus = mutableState.value.sipCallStatus,
                serviceIncomingCallActive = true
            )
        ) return false
        val effectiveCall = call?.takeIf { it.number == serviceCall.number } ?: serviceCall
        notificationIncomingCall = effectiveCall
        mutableState.value = mutableState.value.copy(
            sipCallStatus = SipCallStatus.INCOMING,
            incomingSipCall = effectiveCall,
            sipCallError = null
        )
        if (!mutableState.value.contactsLoaded) loadContacts(false)
        return true
    }

    fun answerIncomingFromNotification(call: IncomingSipCall?): Boolean {
        if (!presentIncomingFromNotification(call)) return false
        if (linphoneEngine?.acceptIncomingCall() == true) {
            answerIncomingWhenReady = false
            SipForegroundService.prepareForAnswer()
            mutableState.value = mutableState.value.copy(incomingSipCall = null)
            telecomController()?.answerFromApp()
            return true
        } else if (
            SipForegroundService.currentIncomingCall() != null &&
            mutableState.value.sipCallStatus == SipCallStatus.INCOMING
        ) {
            answerIncomingWhenReady = true
            mutableState.value = mutableState.value.copy(incomingSipCall = null)
            telecomController()?.answerFromApp()
            return true
        }
        return false
    }

    private fun answerIncomingFromLockScreen(): Boolean =
        answerIncomingFromNotification(SipForegroundService.currentIncomingCall())

    private fun onIncomingNotificationChanged(call: IncomingSipCall?) {
        if (call != null) {
            presentIncomingFromNotification(call)
            return
        }
        notificationIncomingCall = null
        answerIncomingWhenReady = false
        if (
            !sipIncomingWasActive &&
            mutableState.value.sipCallStatus in setOf(
                SipCallStatus.IDLE,
                SipCallStatus.INCOMING,
                SipCallStatus.FAILED
            )
        ) {
            mutableState.value = mutableState.value.copy(
                sipCallStatus = SipCallStatus.IDLE,
                incomingSipCall = null,
                sipCallError = null
            )
        }
    }

    fun rejectIncomingCall() {
        rejectIncomingCall(notifyTelecom = true)
    }

    fun rejectIncomingFromTelecom() {
        rejectIncomingCall(notifyTelecom = false)
    }

    private fun rejectIncomingCall(notifyTelecom: Boolean) {
        if (mutableState.value.sipCallStatus != SipCallStatus.INCOMING) return
        SipForegroundService.markRejected(getApplication())
        if (notifyTelecom) telecomController()?.disconnect(DisconnectCause.REJECTED)
        linphoneEngine?.rejectIncomingCall()
        mutableState.value = mutableState.value.copy(
            sipCallStatus = SipCallStatus.ENDING,
            incomingSipCall = null,
            sipCallError = null
        )
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { api.declineIncomingCall() }
            }
            if (result.isFailure) {
                mutableState.value = mutableState.value.copy(
                    sipCallStatus = SipCallStatus.FAILED,
                    sipCallError = result.exceptionOrNull()?.toFriendlyMessage()
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            SipForegroundService.stop(getApplication())
            linphoneEngine?.clearAccount()
            withContext(Dispatchers.IO) { api.logout() }
            mutableState.value = LoginUiState(
                restoringSession = false,
                sipEngineStatus = mutableState.value.sipEngineStatus
            )
        }
    }

    fun updatePresence(presence: String) {
        if (mutableState.value.updatingPresence || mutableState.value.user == null) return
        mutableState.value = mutableState.value.copy(
            updatingPresence = true,
            presenceError = null
        )
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { api.updatePresence(presence) }
            }
            val error = result.exceptionOrNull()
            mutableState.value = mutableState.value.copy(
                updatingPresence = false,
                user = result.getOrNull() ?: mutableState.value.user,
                presenceError = error?.toFriendlyMessage()
            )
            if (error is ApiException &&
                error.statusCode in listOf(401, 403)
            ) {
                withContext(Dispatchers.IO) { api.logout() }
                mutableState.value = LoginUiState(
                    restoringSession = false,
                    sipEngineStatus = mutableState.value.sipEngineStatus,
                    error = error.toFriendlyMessage()
                )
            }
        }
    }

    fun updateProfile(
        name: String,
        email: String,
        password: String?,
        avatarBytes: ByteArray?,
        avatarFileName: String?,
        avatarContentType: String?
    ) {
        if (mutableState.value.savingProfile || mutableState.value.user == null) return
        mutableState.value = mutableState.value.copy(
            savingProfile = true,
            profileMessage = null,
            profileError = null
        )
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    var updated = api.updateProfile(name, email, password)
                    if (avatarBytes != null && avatarFileName != null && avatarContentType != null) {
                        updated = api.uploadAvatar(
                            avatarBytes,
                            avatarFileName,
                            avatarContentType
                        )
                    }
                    updated
                }
            }
            val error = result.exceptionOrNull()
            mutableState.value = mutableState.value.copy(
                savingProfile = false,
                user = result.getOrNull() ?: mutableState.value.user,
                profileMessage = if (result.isSuccess) "Perfil atualizado." else null,
                profileError = error?.toFriendlyMessage()
            )
        }
    }

    fun clearProfileFeedback() {
        mutableState.value = mutableState.value.copy(
            profileMessage = null,
            profileError = null
        )
    }

    private fun hydrateCachedData(user: AuthenticatedUser) {
        val contacts = api.cachedContacts(user.extension)
        val history = api.cachedHistory(user.extension)
        mutableState.value = mutableState.value.copy(
            contacts = contacts ?: mutableState.value.contacts,
            contactsLoaded = contacts != null,
            history = history ?: mutableState.value.history,
            historyLoaded = history != null
        )
    }

    private fun hydrateAndRefreshCachedData(user: AuthenticatedUser) {
        hydrateCachedData(user)
        loadContactsInternal(force = false, silent = true, allowLoaded = true)
        loadHistoryInternal(force = false, silent = true, allowLoaded = true)
    }

    fun loadContacts(force: Boolean = false) {
        loadContactsInternal(force = force, silent = false, allowLoaded = false)
    }

    private fun loadContactsInternal(
        force: Boolean,
        silent: Boolean,
        allowLoaded: Boolean
    ) {
        val current = mutableState.value
        if (contactsRequestInFlight || current.user == null ||
            (!force && current.contactsLoaded && !allowLoaded)
        ) return
        contactsRequestInFlight = true
        mutableState.value = current.copy(
            loadingContacts = !silent,
            contactsError = null
        )
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { api.contacts(force) }
            }
            val error = result.exceptionOrNull()
            val contacts = result.getOrNull()
            if (contacts != null) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        api.cacheContacts(current.user.extension, contacts)
                    }
                }
            }
            contactsRequestInFlight = false
            mutableState.value = mutableState.value.copy(
                loadingContacts = false,
                contactsLoaded = result.isSuccess,
                contacts = contacts ?: mutableState.value.contacts,
                contactsError = if (mutableState.value.contacts.isEmpty()) {
                    error?.toFriendlyMessage()
                } else null
            )
            if (error is ApiException && error.statusCode in listOf(401, 403)) {
                withContext(Dispatchers.IO) { api.logout() }
                mutableState.value = LoginUiState(
                    restoringSession = false,
                    sipEngineStatus = mutableState.value.sipEngineStatus,
                    error = error.toFriendlyMessage()
                )
            }
        }
    }

    fun loadHistory(force: Boolean = false) {
        loadHistoryInternal(force = force, silent = false, allowLoaded = false)
    }

    private fun loadHistoryInternal(
        force: Boolean,
        silent: Boolean,
        allowLoaded: Boolean
    ) {
        val current = mutableState.value
        if (historyRequestInFlight || current.user == null ||
            (!force && current.historyLoaded && !allowLoaded)
        ) return
        historyRequestInFlight = true
        mutableState.value = current.copy(
            loadingHistory = !silent,
            historyError = null
        )
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { api.history(force) }
            }
            val error = result.exceptionOrNull()
            val history = result.getOrNull()
            if (history != null) {
                withContext(Dispatchers.IO) {
                    runCatching {
                        api.cacheHistory(current.user.extension, history)
                    }
                }
            }
            historyRequestInFlight = false
            mutableState.value = mutableState.value.copy(
                loadingHistory = false,
                historyLoaded = result.isSuccess,
                history = history ?: mutableState.value.history,
                historyError = if (mutableState.value.history.isEmpty()) {
                    error?.toFriendlyMessage()
                } else null
            )
            if (error is ApiException && error.statusCode in listOf(401, 403)) {
                withContext(Dispatchers.IO) { api.logout() }
                mutableState.value = LoginUiState(
                    restoringSession = false,
                    sipEngineStatus = mutableState.value.sipEngineStatus,
                    error = error.toFriendlyMessage()
                )
            }
        }
    }

    fun toggleRecording(call: HistoryCall) {
        if (!call.recording || call.id.isBlank() ||
            mutableState.value.loadingRecordingId != null
        ) return
        if (mutableState.value.activeRecordingId == call.id && mediaPlayer != null) {
            val player = requireNotNull(mediaPlayer)
            if (player.isPlaying) player.pause() else player.start()
            mutableState.value = mutableState.value.copy(
                recordingPlaying = player.isPlaying,
                recordingError = null
            )
            if (player.isPlaying) monitorPlayback(call.id)
            return
        }

        releasePlayer()
        mutableState.value = mutableState.value.copy(
            loadingRecordingId = call.id,
            activeRecordingId = null,
            recordingPlaying = false,
            recordingPosition = 0,
            recordingDuration = 0,
            recordingError = null
        )
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val directory = getApplication<Application>().cacheDir
                        .resolve("recordings")
                    directory.listFiles()?.forEach(File::delete)
                    api.downloadRecording(
                        call.id,
                        directory.resolve("authorized-recording")
                    )
                }
            }
            val file = result.getOrNull()
            if (file == null) {
                mutableState.value = mutableState.value.copy(
                    loadingRecordingId = null,
                    recordingError = result.exceptionOrNull()?.toFriendlyMessage()
                )
                return@launch
            }
            runCatching {
                MediaPlayer().also { player ->
                    player.setDataSource(file.absolutePath)
                    player.prepare()
                    player.setOnCompletionListener {
                        mutableState.value = mutableState.value.copy(
                            recordingPlaying = false,
                            recordingPosition = mutableState.value.recordingDuration
                        )
                    }
                    player.start()
                    mediaPlayer = player
                }
            }.onSuccess {
                val player = requireNotNull(mediaPlayer)
                mutableState.value = mutableState.value.copy(
                    loadingRecordingId = null,
                    activeRecordingId = call.id,
                    recordingPlaying = true,
                    recordingDuration = player.duration.coerceAtLeast(0),
                    recordingPosition = 0
                )
                monitorPlayback(call.id)
            }.onFailure {
                releasePlayer()
                mutableState.value = mutableState.value.copy(
                    loadingRecordingId = null,
                    recordingError = "Não foi possível reproduzir esta gravação."
                )
            }
        }
    }

    fun seekRecording(position: Int) {
        val player = mediaPlayer ?: return
        val target = position.coerceIn(0, player.duration.coerceAtLeast(0))
        player.seekTo(target)
        mutableState.value = mutableState.value.copy(recordingPosition = target)
    }

    private fun monitorPlayback(callId: String) {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (mutableState.value.activeRecordingId == callId) {
                val player = mediaPlayer ?: break
                mutableState.value = mutableState.value.copy(
                    recordingPlaying = player.isPlaying,
                    recordingPosition = player.currentPosition.coerceAtLeast(0),
                    recordingDuration = player.duration.coerceAtLeast(0)
                )
                if (!player.isPlaying) break
                delay(400)
            }
        }
    }

    private fun releasePlayer() {
        playbackJob?.cancel()
        playbackJob = null
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onCleared() {
        SipForegroundService.setAnswerCallHandler(null)
        SipForegroundService.setRejectCallHandler(null)
        SipForegroundService.setHangupCallHandler(null)
        SipForegroundService.setIncomingNotificationHandler(null)
        releasePlayer()
        linphoneEngine?.stop()
        linphoneEngine = null
        super.onCleared()
    }

    private fun Throwable.toFriendlyMessage(): String = when (this) {
        is ApiException -> message ?: "Não foi possível entrar."
        is IOException -> "Não foi possível conectar ao Eagle PBX."
        else -> "O aplicativo não conseguiu concluir o acesso."
    }
}
