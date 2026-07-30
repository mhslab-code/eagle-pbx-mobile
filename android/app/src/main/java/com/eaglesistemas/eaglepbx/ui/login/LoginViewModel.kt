package com.eaglesistemas.eaglepbx.ui.login

import android.app.Application
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
import com.eaglesistemas.eaglepbx.telephony.LinphoneEngine
import com.eaglesistemas.eaglepbx.telephony.IncomingSipCall
import com.eaglesistemas.eaglepbx.telephony.SipCallStatus
import com.eaglesistemas.eaglepbx.telephony.SipEngineStatus
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
    val sipEngineStatus: SipEngineStatus = SipEngineStatus.INITIALIZING,
    val sipCallStatus: SipCallStatus = SipCallStatus.IDLE,
    val incomingSipCall: IncomingSipCall? = null,
    val sipCallError: String? = null,
    val registeringMobileDevice: Boolean = false,
    val mobileDevice: MobileDeviceRegistration? = null,
    val mobileDeviceError: String? = null,
    val user: AuthenticatedUser? = null,
    val error: String? = null,
    val presenceError: String? = null,
    val contactsError: String? = null,
    val historyError: String? = null,
    val recordingError: String? = null
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val api = EagleApiClient(
        SecureSessionStore(application),
        DeviceIdentityStore(application)
    )
    private var linphoneEngine: LinphoneEngine? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null
    private val mutableState = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = mutableState.asStateFlow()

    init {
        initializeSipEngine()
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { api.restoreSession() }
            }
            mutableState.value = LoginUiState(
                restoringSession = false,
                sipEngineStatus = mutableState.value.sipEngineStatus,
                user = result.getOrNull(),
                error = result.exceptionOrNull()?.toFriendlyMessage()
            )
            if (result.getOrNull() != null) registerMobileDevice()
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
            if (result.getOrNull()?.status == "ready") configureSipAccount()
        }
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
                },
                onCallStatusChanged = { status ->
                    mutableState.value = mutableState.value.copy(
                        sipCallStatus = status
                    )
                },
                onIncomingCallChanged = { call ->
                    mutableState.value = mutableState.value.copy(
                        incomingSipCall = call
                    )
                    if (call != null && !mutableState.value.contactsLoaded) {
                        loadContacts(false)
                    }
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
            if (result.getOrNull() != null) registerMobileDevice()
        }
    }

    fun placeCall(destination: String) {
        if (
            mutableState.value.sipEngineStatus != SipEngineStatus.REGISTERED ||
            mutableState.value.sipCallStatus != SipCallStatus.IDLE
        ) return
        if (linphoneEngine?.placeCall(destination) != true) {
            mutableState.value = mutableState.value.copy(
                sipCallStatus = SipCallStatus.FAILED
            )
        }
    }

    fun hangupCall() {
        linphoneEngine?.hangupCall()
    }

    fun acceptIncomingCall() {
        if (mutableState.value.sipCallStatus != SipCallStatus.INCOMING) return
        if (linphoneEngine?.acceptIncomingCall() != true) {
            mutableState.value = mutableState.value.copy(
                sipCallStatus = SipCallStatus.FAILED,
                incomingSipCall = null
            )
        }
    }

    fun rejectIncomingCall() {
        if (mutableState.value.sipCallStatus != SipCallStatus.INCOMING) return
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

    fun loadContacts(force: Boolean = false) {
        val current = mutableState.value
        if (current.loadingContacts || current.user == null ||
            (!force && current.contactsLoaded)
        ) return
        mutableState.value = current.copy(
            loadingContacts = true,
            contactsError = null
        )
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { api.contacts(force) }
            }
            val error = result.exceptionOrNull()
            mutableState.value = mutableState.value.copy(
                loadingContacts = false,
                contactsLoaded = result.isSuccess,
                contacts = result.getOrNull() ?: mutableState.value.contacts,
                contactsError = error?.toFriendlyMessage()
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
        val current = mutableState.value
        if (current.loadingHistory || current.user == null ||
            (!force && current.historyLoaded)
        ) return
        mutableState.value = current.copy(
            loadingHistory = true,
            historyError = null
        )
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { api.history() }
            }
            val error = result.exceptionOrNull()
            mutableState.value = mutableState.value.copy(
                loadingHistory = false,
                historyLoaded = result.isSuccess,
                history = result.getOrNull() ?: mutableState.value.history,
                historyError = error?.toFriendlyMessage()
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
