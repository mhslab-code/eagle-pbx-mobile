package com.eaglesistemas.eaglepbx

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Base64
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.eaglesistemas.eaglepbx.data.AuthenticatedUser
import com.eaglesistemas.eaglepbx.data.EagleContact
import com.eaglesistemas.eaglepbx.data.HistoryCall
import com.eaglesistemas.eaglepbx.ui.theme.EagleBlue
import com.eaglesistemas.eaglepbx.ui.theme.EagleBlueDark
import com.eaglesistemas.eaglepbx.ui.theme.EagleBorder
import com.eaglesistemas.eaglepbx.ui.theme.EagleDanger
import com.eaglesistemas.eaglepbx.ui.theme.EagleNavy
import com.eaglesistemas.eaglepbx.ui.theme.EagleNavyLight
import com.eaglesistemas.eaglepbx.ui.theme.EagleSuccess
import com.eaglesistemas.eaglepbx.ui.theme.EagleText
import com.eaglesistemas.eaglepbx.ui.theme.EagleTextMuted
import com.eaglesistemas.eaglepbx.ui.theme.EaglePBXTheme
import com.eaglesistemas.eaglepbx.ui.login.LoginViewModel
import com.eaglesistemas.eaglepbx.telephony.SipCallStatus
import com.eaglesistemas.eaglepbx.telephony.AttendedTransferStatus
import com.eaglesistemas.eaglepbx.telephony.ConferenceSetupStatus
import com.eaglesistemas.eaglepbx.telephony.SipEngineStatus
import com.eaglesistemas.eaglepbx.telephony.IncomingSipCall
import com.eaglesistemas.eaglepbx.telephony.SipAudioOutput
import com.eaglesistemas.eaglepbx.telephony.SipForegroundService
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val loginViewModel: LoginViewModel by viewModels()
    private var returnToLockScreenAfterCall = false
    private var incomingCallObserved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        handleNotificationAction(intent)
        observeLockedScreenCallEnd()
        enableEdgeToEdge()
        setContent {
            EaglePBXTheme {
                EaglePBXApp(loginViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        loginViewModel.setApplicationInBackground(false)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationAction(intent)
    }

    private fun handleNotificationAction(intent: Intent?) {
        when (intent?.action) {
            SipForegroundService.ACTION_SHOW_INCOMING -> {
                returnToLockScreenAfterCall = true
                intent.action = null
            }
            SipForegroundService.ACTION_ANSWER -> {
                returnToLockScreenAfterCall = true
                loginViewModel.acceptIncomingCall()
                intent.action = null
            }
        }
    }

    private fun observeLockedScreenCallEnd() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                loginViewModel.state
                    .map { it.sipCallStatus }
                    .distinctUntilChanged()
                    .collect { status ->
                        if (status != SipCallStatus.IDLE) {
                            incomingCallObserved = true
                        } else if (
                            returnToLockScreenAfterCall &&
                            incomingCallObserved
                        ) {
                            returnToLockScreenAfterCall = false
                            incomingCallObserved = false
                            moveTaskToBack(true)
                        }
                    }
            }
        }
    }

    override fun onStop() {
        loginViewModel.setApplicationInBackground(true)
        super.onStop()
    }
}

@Composable
fun EaglePBXApp(viewModel: LoginViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    when {
        state.restoringSession -> LoadingScreen()
        state.user != null -> AuthenticatedScreen(
            user = requireNotNull(state.user),
            connectionError = state.connectionError,
            updatingPresence = state.updatingPresence,
            presenceError = state.presenceError,
            contacts = state.contacts,
            loadingContacts = state.loadingContacts,
            contactsError = state.contactsError,
            onLoadContacts = viewModel::loadContacts,
            history = state.history,
            loadingHistory = state.loadingHistory,
            historyError = state.historyError,
            onLoadHistory = viewModel::loadHistory,
            loadingRecordingId = state.loadingRecordingId,
            activeRecordingId = state.activeRecordingId,
            recordingPlaying = state.recordingPlaying,
            recordingPosition = state.recordingPosition,
            recordingDuration = state.recordingDuration,
            recordingError = state.recordingError,
            sipEngineStatus = state.sipEngineStatus,
            sipCallStatus = state.sipCallStatus,
            attendedTransferStatus = state.attendedTransferStatus,
            conferenceSetupStatus = state.conferenceSetupStatus,
            microphoneMuted = state.microphoneMuted,
            audioOutputs = state.audioOutputs,
            incomingSipCall = state.incomingSipCall,
            registeringMobileDevice = state.registeringMobileDevice,
            mobileDeviceStatus = state.mobileDevice?.status,
            mobileDeviceError = state.mobileDeviceError,
            onToggleRecording = viewModel::toggleRecording,
            onSeekRecording = viewModel::seekRecording,
            onPlaceCall = viewModel::placeCall,
            onHangupCall = viewModel::hangupCall,
            onSendDtmf = viewModel::sendDtmf,
            onToggleMicrophone = viewModel::toggleMicrophone,
            onLoadAudioOutputs = viewModel::loadAudioOutputs,
            onSelectAudioOutput = viewModel::selectAudioOutput,
            onToggleCallHold = viewModel::toggleCallHold,
            onTransferDirect = viewModel::transferDirect,
            onStartAttendedTransfer = viewModel::startAttendedTransfer,
            onCancelAttendedTransfer = viewModel::cancelAttendedTransfer,
            onCompleteAttendedTransfer = viewModel::completeAttendedTransfer,
            onStartAdditionalCall = viewModel::startAdditionalCall,
            onCancelAdditionalCall = viewModel::cancelAdditionalCall,
            onCompleteConference = viewModel::completeConference,
            onAcceptIncomingCall = viewModel::acceptIncomingCall,
            onRejectIncomingCall = viewModel::rejectIncomingCall,
            onPresenceChange = viewModel::updatePresence,
            onLogout = viewModel::logout
        )
        else -> LoginScreen(
            submitting = state.submitting,
            error = state.error,
            onLogin = viewModel::login
        )
    }
}

@Composable
fun LoginScreen(
    submitting: Boolean = false,
    error: String? = null,
    onLogin: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var extension by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = EagleNavy,
        unfocusedContainerColor = EagleNavy,
        focusedTextColor = EagleText,
        unfocusedTextColor = EagleText,
        focusedIndicatorColor = EagleBlue,
        unfocusedIndicatorColor = EagleBorder,
        cursorColor = EagleBlue,
        focusedPlaceholderColor = EagleTextMuted,
        unfocusedPlaceholderColor = EagleTextMuted
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EagleNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.eagle_pbx_logo),
                contentDescription = "Eagle Sistemas",
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Eagle PBX",
                color = EagleText,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "TELEFONIA CORPORATIVA",
                color = EagleBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(34.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(EagleNavyLight)
                    .border(1.dp, EagleBorder, RoundedCornerShape(24.dp))
                    .padding(22.dp)
            ) {
                Text(
                    text = "Bem-vindo",
                    color = EagleText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Acesse seu ramal, agenda e histórico.",
                    color = EagleTextMuted,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Ramal",
                    color = EagleText,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = extension,
                    onValueChange = { extension = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Digite o seu ramal") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    enabled = !submitting,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    colors = fieldColors
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Senha",
                    color = EagleText,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("••••••••") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !submitting,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onLogin(extension, password) }
                    ),
                    colors = fieldColors
                )
                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = error,
                        color = EagleDanger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { onLogin(extension, password) },
                    enabled = !submitting && extension.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EagleBlueDark,
                        contentColor = EagleText
                    )
                ) {
                    if (submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = EagleText,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Entrar",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Versão ${BuildConfig.VERSION_NAME} · Acesso seguro",
                modifier = Modifier.fillMaxWidth(),
                color = EagleTextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EagleNavy),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = EagleBlue)
    }
}

@Composable
fun AuthenticatedScreen(
    user: AuthenticatedUser,
    connectionError: String?,
    updatingPresence: Boolean,
    presenceError: String?,
    contacts: List<EagleContact>,
    loadingContacts: Boolean,
    contactsError: String?,
    onLoadContacts: (Boolean) -> Unit,
    history: List<HistoryCall>,
    loadingHistory: Boolean,
    historyError: String?,
    onLoadHistory: (Boolean) -> Unit,
    loadingRecordingId: String?,
    activeRecordingId: String?,
    recordingPlaying: Boolean,
    recordingPosition: Int,
    recordingDuration: Int,
    recordingError: String?,
    sipEngineStatus: SipEngineStatus,
    sipCallStatus: SipCallStatus,
    attendedTransferStatus: AttendedTransferStatus,
    conferenceSetupStatus: ConferenceSetupStatus,
    microphoneMuted: Boolean,
    audioOutputs: List<SipAudioOutput>,
    incomingSipCall: IncomingSipCall?,
    registeringMobileDevice: Boolean,
    mobileDeviceStatus: String?,
    mobileDeviceError: String?,
    onToggleRecording: (HistoryCall) -> Unit,
    onSeekRecording: (Int) -> Unit,
    onPlaceCall: (String) -> Unit,
    onHangupCall: () -> Unit,
    onSendDtmf: (Char) -> Unit,
    onToggleMicrophone: () -> Unit,
    onLoadAudioOutputs: () -> Unit,
    onSelectAudioOutput: (String) -> Unit,
    onToggleCallHold: () -> Unit,
    onTransferDirect: (String) -> Boolean,
    onStartAttendedTransfer: (String) -> Boolean,
    onCancelAttendedTransfer: () -> Boolean,
    onCompleteAttendedTransfer: () -> Boolean,
    onStartAdditionalCall: (String) -> Boolean,
    onCancelAdditionalCall: () -> Boolean,
    onCompleteConference: () -> Boolean,
    onAcceptIncomingCall: () -> Unit,
    onRejectIncomingCall: () -> Unit,
    onPresenceChange: (String) -> Unit,
    onLogout: () -> Unit
) {
    var presenceMenuOpen by remember { mutableStateOf(false) }
    var accountDialogOpen by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf(MainSection.DIALER) }
    val presenceLabel = if (connectionError != null) "Sem conexão" else when (user.presence) {
        "dnd" -> "Não perturbe"
        "offline" -> "Offline"
        else -> "Online"
    }
    val presenceColor = if (connectionError != null) EagleDanger else when (user.presence) {
        "dnd" -> EagleDanger
        "offline" -> EagleTextMuted
        else -> EagleSuccess
    }

    LaunchedEffect(selectedSection) {
        if (selectedSection == MainSection.CONTACTS) onLoadContacts(false)
        if (selectedSection == MainSection.HISTORY) onLoadHistory(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EagleNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
        ) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.eagle_pbx_logo),
                    contentDescription = "Eagle Sistemas",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(13.dp))
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .clickable { accountDialogOpen = true }
                ) {
                    Text(
                        text = user.name,
                        color = EagleText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ramal ${user.extension}",
                        color = EagleTextMuted,
                        fontSize = 14.sp
                    )
                }
                Box {
                    Button(
                        onClick = { presenceMenuOpen = true },
                        enabled = !updatingPresence,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EagleNavyLight,
                            contentColor = presenceColor
                        )
                    ) {
                        if (updatingPresence) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = EagleBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = presenceLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = presenceMenuOpen,
                        onDismissRequest = { presenceMenuOpen = false }
                    ) {
                        PresenceMenuItem("Online", "online") {
                            presenceMenuOpen = false
                            onPresenceChange(it)
                        }
                        PresenceMenuItem("Não perturbe", "dnd") {
                            presenceMenuOpen = false
                            onPresenceChange(it)
                        }
                        PresenceMenuItem("Offline", "offline") {
                            presenceMenuOpen = false
                            onPresenceChange(it)
                        }
                    }
                }
            }
            if (!presenceError.isNullOrBlank()) {
                Text(
                    text = presenceError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    color = EagleDanger,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(EagleNavyLight)
                    .border(1.dp, EagleBorder, RoundedCornerShape(22.dp))
                    .padding(
                        horizontal = if (selectedSection == MainSection.DIALER) 14.dp else 22.dp,
                        vertical = if (selectedSection == MainSection.DIALER) 14.dp else 22.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (selectedSection) {
                    MainSection.DIALER -> DialerContent(
                        sipEngineStatus = sipEngineStatus,
                        sipCallStatus = sipCallStatus,
                        attendedTransferStatus = attendedTransferStatus,
                        conferenceSetupStatus = conferenceSetupStatus,
                        microphoneMuted = microphoneMuted,
                        audioOutputs = audioOutputs,
                        registeringMobileDevice = registeringMobileDevice,
                        mobileDeviceStatus = mobileDeviceStatus,
                        mobileDeviceError = mobileDeviceError,
                        onPlaceCall = onPlaceCall,
                        onHangupCall = onHangupCall,
                        onSendDtmf = onSendDtmf,
                        onToggleMicrophone = onToggleMicrophone,
                        onLoadAudioOutputs = onLoadAudioOutputs,
                        onSelectAudioOutput = onSelectAudioOutput,
                        onToggleCallHold = onToggleCallHold,
                        onTransferDirect = onTransferDirect,
                        onStartAttendedTransfer = onStartAttendedTransfer,
                        onCancelAttendedTransfer = onCancelAttendedTransfer,
                        onCompleteAttendedTransfer = onCompleteAttendedTransfer,
                        onStartAdditionalCall = onStartAdditionalCall,
                        onCancelAdditionalCall = onCancelAdditionalCall,
                        onCompleteConference = onCompleteConference
                    )
                    MainSection.CONTACTS -> ContactsContent(
                        contacts = contacts,
                        loading = loadingContacts,
                        error = contactsError,
                        onRefresh = { onLoadContacts(true) }
                    )
                    MainSection.HISTORY -> HistoryContent(
                        calls = history,
                        loading = loadingHistory,
                        error = historyError,
                        onRefresh = { onLoadHistory(true) },
                        loadingRecordingId = loadingRecordingId,
                        activeRecordingId = activeRecordingId,
                        recordingPlaying = recordingPlaying,
                        recordingPosition = recordingPosition,
                        recordingDuration = recordingDuration,
                        recordingError = recordingError,
                        onToggleRecording = onToggleRecording,
                        onSeekRecording = onSeekRecording
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(EagleNavyLight)
                    .border(1.dp, EagleBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MainSection.entries.forEach { section ->
                    TextButton(
                        onClick = { selectedSection = section },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (selectedSection == section) {
                                EagleBlue
                            } else {
                                EagleTextMuted
                            }
                        )
                    ) {
                        Text(
                            text = section.shortTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
    if (accountDialogOpen) {
        AccountDialog(
            user = user,
            onDismiss = { accountDialogOpen = false },
            onLogout = {
                accountDialogOpen = false
                onLogout()
            }
        )
    }
    if (sipCallStatus == SipCallStatus.INCOMING && incomingSipCall != null) {
        IncomingCallDialog(
            call = incomingSipCall,
            contacts = contacts,
            onAccept = onAcceptIncomingCall,
            onReject = onRejectIncomingCall
        )
    }
}

@Composable
private fun PreparedSection(section: MainSection) {
    Text(
        text = section.title,
        color = EagleText,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = section.description,
        color = EagleTextMuted,
        fontSize = 15.sp,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(22.dp))
    Text(
        text = "Estrutura preparada",
        color = EagleSuccess,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ContactsContent(
    contacts: List<EagleContact>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    var selectedContact by remember { mutableStateOf<EagleContact?>(null) }
    val filtered = remember(contacts, search) {
        val query = search.trim().lowercase()
        if (query.isBlank()) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.name.lowercase().contains(query) ||
                    contact.numbers.any {
                        it.number.lowercase().contains(query) ||
                            it.label.lowercase().contains(query)
                    }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Contatos",
                color = EagleText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${contacts.size} contato${if (contacts.size == 1) "" else "s"}",
                color = EagleTextMuted,
                fontSize = 12.sp
            )
        }
        Button(
            onClick = onRefresh,
            enabled = !loading,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EagleNavy,
                contentColor = EagleBlue
            )
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = EagleBlue,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Atualizar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = search,
        onValueChange = { search = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Pesquisar nome ou número") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = EagleNavy,
            unfocusedContainerColor = EagleNavy,
            focusedTextColor = EagleText,
            unfocusedTextColor = EagleText,
            focusedIndicatorColor = EagleBlue,
            unfocusedIndicatorColor = EagleBorder,
            cursorColor = EagleBlue,
            focusedPlaceholderColor = EagleTextMuted,
            unfocusedPlaceholderColor = EagleTextMuted
        )
    )
    Spacer(Modifier.height(10.dp))
    when {
        loading && contacts.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = EagleBlue)
        }
        !error.isNullOrBlank() && contacts.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = error,
                color = EagleDanger,
                textAlign = TextAlign.Center
            )
        }
        filtered.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (contacts.isEmpty()) {
                    "Nenhum contato disponível."
                } else {
                    "Nenhum contato corresponde à busca."
                },
                color = EagleTextMuted,
                textAlign = TextAlign.Center
            )
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.name.lowercase() }) { contact ->
                ContactCard(
                    contact = contact,
                    onClick = { selectedContact = contact }
                )
            }
        }
    }

    selectedContact?.let { contact ->
        ContactNumbersDialog(
            contact = contact,
            onDismiss = { selectedContact = null }
        )
    }
}

@Composable
private fun ContactCard(
    contact: EagleContact,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EagleNavy)
            .border(1.dp, EagleBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(contact)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = contact.name,
                color = EagleText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when (contact.numbers.size) {
                    0 -> "Sem telefone"
                    1 -> contact.numbers.first().number
                    else -> "${contact.numbers.size} números disponíveis"
                },
                color = EagleTextMuted,
                fontSize = 12.sp
            )
        }
        Text(
            text = "☎",
            color = if (contact.numbers.isEmpty()) EagleTextMuted else EagleSuccess,
            fontSize = 20.sp
        )
    }
}

@Composable
private fun ContactAvatar(contact: EagleContact) {
    val bitmap = remember(contact.photo) {
        val encoded = contact.photo
            ?.takeIf { it.startsWith("data:image/") && it.contains(",") }
            ?.substringAfter(',')
        encoded?.let {
            runCatching {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(EagleBlueDark),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Foto de ${contact.name}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = contact.name
                    .split(' ')
                    .filter(String::isNotBlank)
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .joinToString("")
                    .ifBlank { "EP" },
                color = EagleText,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ContactNumbersDialog(
    contact: EagleContact,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = contact.name,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (contact.numbers.isEmpty()) {
                    Text("Nenhum telefone disponível.", color = EagleTextMuted)
                } else {
                    contact.numbers.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, EagleBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.number,
                                    color = EagleText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = entry.label,
                                    color = EagleTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                            Text("☎", color = EagleSuccess, fontSize = 19.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        },
        containerColor = EagleNavyLight,
        textContentColor = EagleText,
        titleContentColor = EagleText
    )
}

@Composable
private fun HistoryContent(
    calls: List<HistoryCall>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    loadingRecordingId: String?,
    activeRecordingId: String?,
    recordingPlaying: Boolean,
    recordingPosition: Int,
    recordingDuration: Int,
    recordingError: String?,
    onToggleRecording: (HistoryCall) -> Unit,
    onSeekRecording: (Int) -> Unit
) {
    var missedOnly by rememberSaveable { mutableStateOf(false) }
    val filtered = remember(calls, missedOnly) {
        if (missedOnly) calls.filter(HistoryCall::missed) else calls
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Histórico",
                color = EagleText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Acompanhe suas chamadas recentes.",
                color = EagleTextMuted,
                fontSize = 12.sp
            )
        }
        Button(
            onClick = onRefresh,
            enabled = !loading,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EagleNavy,
                contentColor = EagleBlue
            )
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = EagleBlue,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Atualizar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(EagleNavy)
            .border(1.dp, EagleBorder, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HistoryFilter(
            label = "Todas",
            selected = !missedOnly,
            modifier = Modifier.weight(1f),
            onClick = { missedOnly = false }
        )
        HistoryFilter(
            label = "Perdidas",
            selected = missedOnly,
            modifier = Modifier.weight(1f),
            onClick = { missedOnly = true }
        )
    }
    Spacer(Modifier.height(10.dp))
    if (!recordingError.isNullOrBlank()) {
        Text(
            text = recordingError,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            color = EagleDanger,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
    when {
        loading && calls.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = EagleBlue)
        }
        !error.isNullOrBlank() && calls.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(error, color = EagleDanger, textAlign = TextAlign.Center)
        }
        filtered.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (missedOnly) {
                    "Nenhuma chamada perdida."
                } else {
                    "Nenhuma ligação registrada neste aplicativo."
                },
                color = EagleTextMuted,
                textAlign = TextAlign.Center
            )
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { call ->
                HistoryCard(
                    call = call,
                    loading = loadingRecordingId == call.id,
                    active = activeRecordingId == call.id,
                    playing = activeRecordingId == call.id && recordingPlaying,
                    position = if (activeRecordingId == call.id) recordingPosition else 0,
                    playerDuration = if (activeRecordingId == call.id) recordingDuration else 0,
                    onToggle = { onToggleRecording(call) },
                    onSeek = onSeekRecording
                )
            }
        }
    }
}

@Composable
private fun HistoryFilter(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) EagleBlueDark else EagleNavy)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) EagleText else EagleTextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HistoryCard(
    call: HistoryCall,
    loading: Boolean,
    active: Boolean,
    playing: Boolean,
    position: Int,
    playerDuration: Int,
    onToggle: () -> Unit,
    onSeek: (Int) -> Unit
) {
    val displayName = call.remoteName.ifBlank {
        call.remoteNumber.ifBlank { "Número desconhecido" }
    }
    val directionLabel = when {
        call.missed -> "Perdida"
        call.direction == "in" -> "Recebida"
        else -> "Efetuada"
    }
    val directionSymbol = if (call.direction == "in") "↙" else "↗"
    val directionColor = when {
        call.missed -> EagleDanger
        call.direction == "in" -> EagleSuccess
        else -> EagleBlue
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EagleNavy)
            .border(1.dp, EagleBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ContactAvatar(
                EagleContact(
                    name = displayName,
                    numbers = emptyList(),
                    photo = call.remoteAvatar
                )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                Text(
                    text = "$directionSymbol $displayName",
                    color = directionColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$directionLabel · ${formatHistoryDate(call.startedAt)}",
                    color = EagleTextMuted,
                    fontSize = 11.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "DURAÇÃO",
                    color = EagleTextMuted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatDuration(call.durationSeconds),
                    color = EagleText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(EagleBorder)
        )
        Spacer(Modifier.height(9.dp))
        if (call.recording) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(1.dp, EagleBlue, CircleShape)
                        .clickable(enabled = !loading, onClick = onToggle),
                    contentAlignment = Alignment.Center
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(17.dp),
                            color = EagleBlue,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (playing) "Ⅱ" else "▶",
                            color = EagleBlue,
                            fontSize = 13.sp
                        )
                    }
                }
                Slider(
                    value = position.toFloat(),
                    onValueChange = { onSeek(it.toInt()) },
                    enabled = active && playerDuration > 0,
                    valueRange = 0f..playerDuration.coerceAtLeast(1).toFloat(),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 7.dp)
                )
                Text(
                    text = if (active) {
                        "${formatMilliseconds(position)} / ${formatMilliseconds(playerDuration)}"
                    } else {
                        "0:00 / --:--"
                    },
                    color = EagleTextMuted,
                    fontSize = 9.sp
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⌁", color = EagleTextMuted, fontSize = 15.sp)
                Spacer(Modifier.width(9.dp))
                Text(
                    text = "Gravação indisponível",
                    color = EagleTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

private fun formatMilliseconds(milliseconds: Int): String =
    formatDuration(milliseconds.coerceAtLeast(0) / 1000)

private fun formatHistoryDate(value: String): String {
    if (value.isBlank()) return ""
    return runCatching {
        OffsetDateTime.parse(value).format(
            DateTimeFormatter.ofPattern(
                "dd/MM/yyyy, HH:mm:ss",
                Locale.forLanguageTag("pt-BR")
            )
        )
    }.getOrDefault(value)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialerContent(
    sipEngineStatus: SipEngineStatus,
    sipCallStatus: SipCallStatus,
    attendedTransferStatus: AttendedTransferStatus,
    conferenceSetupStatus: ConferenceSetupStatus,
    microphoneMuted: Boolean,
    audioOutputs: List<SipAudioOutput>,
    registeringMobileDevice: Boolean,
    mobileDeviceStatus: String?,
    mobileDeviceError: String?,
    onPlaceCall: (String) -> Unit,
    onHangupCall: () -> Unit,
    onSendDtmf: (Char) -> Unit,
    onToggleMicrophone: () -> Unit,
    onLoadAudioOutputs: () -> Unit,
    onSelectAudioOutput: (String) -> Unit,
    onToggleCallHold: () -> Unit,
    onTransferDirect: (String) -> Boolean,
    onStartAttendedTransfer: (String) -> Boolean,
    onCancelAttendedTransfer: () -> Boolean,
    onCompleteAttendedTransfer: () -> Boolean,
    onStartAdditionalCall: (String) -> Boolean,
    onCancelAdditionalCall: () -> Boolean,
    onCompleteConference: () -> Boolean
) {
    var number by rememberSaveable { mutableStateOf("") }
    var dtmfDigits by rememberSaveable { mutableStateOf("") }
    var audioDialogOpen by rememberSaveable { mutableStateOf(false) }
    var transferDialogOpen by rememberSaveable { mutableStateOf(false) }
    var addCallDialogOpen by rememberSaveable { mutableStateOf(false) }
    val callActive = sipCallStatus in setOf(
        SipCallStatus.INCOMING,
        SipCallStatus.OUTGOING,
        SipCallStatus.RINGING,
        SipCallStatus.CONNECTED,
        SipCallStatus.HELD,
        SipCallStatus.ENDING
    )
    LaunchedEffect(sipCallStatus) {
        if (sipCallStatus == SipCallStatus.IDLE ||
            sipCallStatus == SipCallStatus.OUTGOING
        ) {
            dtmfDigits = ""
        }
    }
    val keys = listOf(
        DialKey("1"), DialKey("2", "ABC"), DialKey("3", "DEF"),
        DialKey("4", "GHI"), DialKey("5", "JKL"), DialKey("6", "MNO"),
        DialKey("7", "PQRS"), DialKey("8", "TUV"), DialKey("9", "WXYZ"),
        DialKey("*"), DialKey("0", "+"), DialKey("#")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, EagleBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = number.ifBlank { "Digite o ramal ou telefone" },
                color = if (number.isBlank()) EagleTextMuted else EagleText,
                fontSize = when {
                    number.length > 22 -> 19.sp
                    number.length > 15 -> 24.sp
                    else -> 31.sp
                },
                fontWeight = if (number.isBlank()) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            if (dtmfDigits.isNotEmpty()) {
                Text(
                    text = "DTMF: $dtmfDigits",
                    color = EagleBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(13.dp))
                .border(1.dp, EagleBorder, RoundedCornerShape(13.dp))
                .combinedClickable(
                    enabled = number.isNotEmpty(),
                    onClick = { number = number.dropLast(1) },
                    onLongClick = { number = "" }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⌫",
                color = if (number.isEmpty()) EagleTextMuted else EagleText,
                fontSize = 24.sp
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    keys.chunked(3).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            row.forEach { key ->
                DialKeyButton(
                    key = key,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (sipCallStatus == SipCallStatus.CONNECTED) {
                            dtmfDigits = (dtmfDigits + key.digit).takeLast(32)
                            onSendDtmf(key.digit.first())
                        } else {
                            number += key.digit
                        }
                    },
                    onLongClick = {
                        if (
                            key.digit == "0" &&
                            sipCallStatus != SipCallStatus.CONNECTED
                        ) {
                            number += "+"
                        }
                    }
                )
            }
        }
        Spacer(Modifier.height(7.dp))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        DialActionButton(
            symbol = "♩",
            label = if (microphoneMuted) "Mudo" else "Microfone",
            enabled = sipCallStatus == SipCallStatus.CONNECTED,
            danger = microphoneMuted,
            onClick = onToggleMicrophone,
            modifier = Modifier.weight(1f)
        )
        DialActionButton(
            symbol = "☎",
            label = "",
            primary = true,
            danger = callActive,
            enabled = (
                callActive ||
                    (
                        sipEngineStatus == SipEngineStatus.REGISTERED &&
                            number.isNotBlank()
                    )
                ),
            onClick = {
                if (callActive) {
                    onHangupCall()
                } else {
                    onPlaceCall(number)
                }
            },
            modifier = Modifier.weight(1f)
        )
        DialActionButton(
            symbol = "◖))",
            label = "Áudio",
            enabled = sipCallStatus == SipCallStatus.CONNECTED,
            onClick = {
                onLoadAudioOutputs()
                audioDialogOpen = true
            },
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(7.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        DialActionButton(
            symbol = "↦",
            label = "Transferir",
            enabled = sipCallStatus == SipCallStatus.CONNECTED,
            onClick = { transferDialogOpen = true },
            modifier = Modifier.weight(1f)
        )
        DialActionButton(
            symbol = if (sipCallStatus == SipCallStatus.HELD) "▶" else "Ⅱ",
            label = if (sipCallStatus == SipCallStatus.HELD) "Retomar" else "Espera",
            enabled = sipCallStatus in setOf(SipCallStatus.CONNECTED, SipCallStatus.HELD),
            primary = sipCallStatus == SipCallStatus.HELD,
            onClick = onToggleCallHold,
            modifier = Modifier.weight(1f)
        )
        DialActionButton(
            "☎+",
            "Adicionar",
            enabled = sipCallStatus == SipCallStatus.CONNECTED &&
                conferenceSetupStatus != ConferenceSetupStatus.ACTIVE,
            onClick = { addCallDialogOpen = true },
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(8.dp))
    if (sipCallStatus != SipCallStatus.IDLE) {
        Text(
            text = when (sipCallStatus) {
                SipCallStatus.OUTGOING -> "Iniciando chamada..."
                SipCallStatus.INCOMING -> "Chamada recebida"
                SipCallStatus.RINGING -> "Chamando..."
                SipCallStatus.CONNECTED -> "Em chamada"
                SipCallStatus.HELD -> "Chamada em espera"
                SipCallStatus.ENDING -> "Encerrando chamada..."
                SipCallStatus.FAILED -> "Não foi possível completar a chamada"
                SipCallStatus.IDLE -> ""
            },
            color = if (sipCallStatus == SipCallStatus.FAILED) {
                EagleDanger
            } else {
                EagleSuccess
            },
            fontSize = 11.sp
        )
        Spacer(Modifier.height(3.dp))
    }
    Text(
        text = when (sipEngineStatus) {
            SipEngineStatus.INITIALIZING -> "Inicializando motor SIP..."
            SipEngineStatus.READY -> "Motor SIP inicializado"
            SipEngineStatus.REGISTERING -> "Registrando telefone..."
            SipEngineStatus.REGISTERED -> "Telefone SIP registrado"
            SipEngineStatus.REGISTRATION_FAILED -> "Falha no registro SIP"
            SipEngineStatus.UNAVAILABLE -> "Motor SIP indisponível"
        },
        color = if (
            sipEngineStatus == SipEngineStatus.READY ||
            sipEngineStatus == SipEngineStatus.REGISTERED
        ) {
            EagleSuccess
        } else if (sipEngineStatus == SipEngineStatus.REGISTRATION_FAILED) {
            EagleDanger
        } else {
            EagleTextMuted
        },
        fontSize = 11.sp
    )
    Spacer(Modifier.height(3.dp))
    Text(
        text = when {
            registeringMobileDevice -> "Registrando dispositivo..."
            mobileDeviceStatus == "ready" -> "Dispositivo SIP provisionado"
            mobileDeviceStatus == "pending" -> "Dispositivo registrado · SIP pendente"
            !mobileDeviceError.isNullOrBlank() -> "Falha ao registrar dispositivo"
            else -> "Identidade do dispositivo pendente"
        },
        color = when {
            mobileDeviceStatus == "ready" -> EagleSuccess
            !mobileDeviceError.isNullOrBlank() -> EagleDanger
            else -> EagleTextMuted
        },
        fontSize = 11.sp
    )
    if (audioDialogOpen) {
        AudioOutputDialog(
            outputs = audioOutputs,
            onSelect = {
                onSelectAudioOutput(it)
                audioDialogOpen = false
            },
            onDismiss = { audioDialogOpen = false }
        )
    }
    if (transferDialogOpen) {
        TransferDialog(
            status = attendedTransferStatus,
            onTransferDirect = onTransferDirect,
            onStartAttended = onStartAttendedTransfer,
            onCancelAttended = onCancelAttendedTransfer,
            onCompleteAttended = onCompleteAttendedTransfer,
            onDismiss = { transferDialogOpen = false }
        )
    }
    if (addCallDialogOpen) {
        AddCallDialog(
            status = conferenceSetupStatus,
            onStart = onStartAdditionalCall,
            onCancelCall = onCancelAdditionalCall,
            onJoin = onCompleteConference,
            onDismiss = { addCallDialogOpen = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransferDialog(
    status: AttendedTransferStatus,
    onTransferDirect: (String) -> Boolean,
    onStartAttended: (String) -> Boolean,
    onCancelAttended: () -> Boolean,
    onCompleteAttended: () -> Boolean,
    onDismiss: () -> Unit
) {
    var destination by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var consultationWasStarted by remember { mutableStateOf(false) }
    val keys = listOf(
        DialKey("1"), DialKey("2", "ABC"), DialKey("3", "DEF"),
        DialKey("4", "GHI"), DialKey("5", "JKL"), DialKey("6", "MNO"),
        DialKey("7", "PQRS"), DialKey("8", "TUV"), DialKey("9", "WXYZ"),
        DialKey("*"), DialKey("0", "+"), DialKey("#")
    )
    LaunchedEffect(status) {
        if (status == AttendedTransferStatus.CALLING) {
            consultationWasStarted = true
        } else if (
            status == AttendedTransferStatus.FAILED && consultationWasStarted
        ) {
            delay(1800)
            onDismiss()
        } else if (
            status == AttendedTransferStatus.IDLE && consultationWasStarted
        ) {
            onDismiss()
        }
    }
    AlertDialog(
        onDismissRequest = {
            if (status == AttendedTransferStatus.IDLE ||
                status == AttendedTransferStatus.FAILED
            ) onDismiss()
        },
        title = {
            Text(
                if (status in setOf(
                        AttendedTransferStatus.CALLING,
                        AttendedTransferStatus.CONNECTED,
                        AttendedTransferStatus.COMPLETING
                    )
                ) "Transferência assistida" else "Transferir chamada",
                color = EagleText
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                val consulting = status in setOf(
                    AttendedTransferStatus.CALLING,
                    AttendedTransferStatus.CONNECTED,
                    AttendedTransferStatus.COMPLETING
                )
                if (consulting) {
                    Text(
                        text = when (status) {
                            AttendedTransferStatus.CALLING ->
                                "Consultando $destination. A chamada original está em espera."
                            AttendedTransferStatus.CONNECTED ->
                                "Consulta estabelecida. Converse com o destino e escolha como continuar."
                            AttendedTransferStatus.COMPLETING ->
                                "Concluindo a transferência..."
                            else -> ""
                        },
                        color = EagleTextMuted,
                        fontSize = 13.sp
                    )
                } else Text(
                        text = "Digite o ramal ou telefone de destino.",
                        color = EagleTextMuted,
                        fontSize = 13.sp
                    )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .border(1.dp, EagleBorder, RoundedCornerShape(13.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = destination.ifBlank { "Ramal ou telefone" },
                        color = if (destination.isBlank()) EagleTextMuted else EagleText,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "⌫",
                        color = if (destination.isBlank()) EagleTextMuted else EagleText,
                        fontSize = 22.sp,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                if (destination.isNotBlank() && !consulting) {
                                    destination = destination.dropLast(1)
                                }
                            },
                            onLongClick = {
                                if (!consulting) destination = ""
                            }
                        )
                    )
                }
                keys.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { key ->
                            DialKeyButton(
                                key = key,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                onClick = {
                                    if (!consulting) destination += key.digit
                                },
                                onLongClick = {
                                    if (!consulting && key.digit == "0") destination += "+"
                                }
                            )
                        }
                    }
                }
                val visibleError = error ?: if (
                    status == AttendedTransferStatus.FAILED &&
                    consultationWasStarted
                ) {
                    "$destination não atendeu. A chamada original foi retomada."
                } else {
                    null
                }
                if (!visibleError.isNullOrBlank()) {
                    Text(
                        text = visibleError,
                        color = if (
                            status == AttendedTransferStatus.FAILED &&
                            error == null
                        ) EagleSuccess else EagleDanger,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = {
                        if (destination.isNotBlank() &&
                            status in setOf(
                                AttendedTransferStatus.IDLE,
                                AttendedTransferStatus.FAILED
                            )
                        ) {
                            if (onTransferDirect(destination)) onDismiss()
                            else error = "Não foi possível transferir a chamada."
                        }
                    }
                ) {
                    Text(
                        "Direta",
                        color = if (status in setOf(
                                AttendedTransferStatus.IDLE,
                                AttendedTransferStatus.FAILED
                            )
                        ) EagleTextMuted else Color.Transparent
                    )
                }
                TextButton(
                    onClick = {
                        if (status == AttendedTransferStatus.CONNECTED) {
                            if (onCompleteAttended()) onDismiss()
                            else error = "Não foi possível concluir a transferência."
                        } else if (destination.isNotBlank() &&
                            status in setOf(
                                AttendedTransferStatus.IDLE,
                                AttendedTransferStatus.FAILED
                            )
                        ) {
                            if (!onStartAttended(destination)) {
                                error = "Não foi possível iniciar a consulta."
                            }
                        }
                    }
                ) {
                    Text(
                        if (status == AttendedTransferStatus.CONNECTED) {
                            "Concluir transferência"
                        } else {
                            "Assistida"
                        },
                        color = EagleBlue
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (status in setOf(
                            AttendedTransferStatus.CALLING,
                            AttendedTransferStatus.CONNECTED
                        )
                    ) {
                        onCancelAttended()
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(
                    if (status in setOf(
                            AttendedTransferStatus.CALLING,
                            AttendedTransferStatus.CONNECTED
                        )
                    ) "Cancelar consulta" else "Cancelar",
                    color = EagleTextMuted
                )
            }
        },
        containerColor = EagleNavyLight
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddCallDialog(
    status: ConferenceSetupStatus,
    onStart: (String) -> Boolean,
    onCancelCall: () -> Boolean,
    onJoin: () -> Boolean,
    onDismiss: () -> Unit
) {
    var destination by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var callWasStarted by remember { mutableStateOf(false) }
    val consulting = status in setOf(
        ConferenceSetupStatus.CALLING,
        ConferenceSetupStatus.CONNECTED,
        ConferenceSetupStatus.JOINING
    )
    val keys = listOf(
        DialKey("1"), DialKey("2", "ABC"), DialKey("3", "DEF"),
        DialKey("4", "GHI"), DialKey("5", "JKL"), DialKey("6", "MNO"),
        DialKey("7", "PQRS"), DialKey("8", "TUV"), DialKey("9", "WXYZ"),
        DialKey("*"), DialKey("0", "+"), DialKey("#")
    )
    LaunchedEffect(status) {
        if (status == ConferenceSetupStatus.CALLING) {
            callWasStarted = true
        } else if (status == ConferenceSetupStatus.FAILED && callWasStarted) {
            delay(1800)
            onDismiss()
        } else if (status == ConferenceSetupStatus.IDLE && callWasStarted) {
            onDismiss()
        }
    }
    AlertDialog(
        onDismissRequest = {
            if (status in setOf(
                    ConferenceSetupStatus.IDLE,
                    ConferenceSetupStatus.FAILED
                )
            ) onDismiss()
        },
        title = {
            Text(
                if (consulting) "Adicionar participante" else "Adicionar chamada",
                color = EagleText
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(
                    text = when (status) {
                        ConferenceSetupStatus.CALLING ->
                            "Chamando $destination. O primeiro participante está em espera."
                        ConferenceSetupStatus.CONNECTED ->
                            "Segundo participante conectado. Forme a conferência ou cancele."
                        ConferenceSetupStatus.JOINING -> "Formando conferência..."
                        else -> "Digite o ramal ou telefone do novo participante."
                    },
                    color = EagleTextMuted,
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .border(1.dp, EagleBorder, RoundedCornerShape(13.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = destination.ifBlank { "Ramal ou telefone" },
                        color = if (destination.isBlank()) EagleTextMuted else EagleText,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "⌫",
                        color = if (destination.isBlank()) EagleTextMuted else EagleText,
                        fontSize = 22.sp,
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                if (destination.isNotBlank() && !consulting) {
                                    destination = destination.dropLast(1)
                                }
                            },
                            onLongClick = {
                                if (!consulting) destination = ""
                            }
                        )
                    )
                }
                keys.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { key ->
                            DialKeyButton(
                                key = key,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                onClick = {
                                    if (!consulting) destination += key.digit
                                },
                                onLongClick = {
                                    if (!consulting && key.digit == "0") destination += "+"
                                }
                            )
                        }
                    }
                }
                val visibleError = error ?: if (
                    status == ConferenceSetupStatus.FAILED && callWasStarted
                ) {
                    "$destination não atendeu. A chamada original foi retomada."
                } else {
                    null
                }
                if (!visibleError.isNullOrBlank()) {
                    Text(
                        visibleError,
                        color = if (
                            status == ConferenceSetupStatus.FAILED && error == null
                        ) EagleSuccess else EagleDanger,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (status == ConferenceSetupStatus.CONNECTED) {
                        if (onJoin()) onDismiss()
                        else error = "Não foi possível formar a conferência."
                    } else if (destination.isNotBlank() &&
                        status in setOf(
                            ConferenceSetupStatus.IDLE,
                            ConferenceSetupStatus.FAILED
                        )
                    ) {
                        if (!onStart(destination)) {
                            error = "Não foi possível iniciar a nova chamada."
                        }
                    }
                }
            ) {
                Text(
                    if (status == ConferenceSetupStatus.CONNECTED) {
                        "Formar conferência"
                    } else if (status == ConferenceSetupStatus.CALLING) {
                        "Aguardando atendimento..."
                    } else {
                        "Adicionar à chamada"
                    },
                    color = if (status == ConferenceSetupStatus.CALLING) {
                        EagleTextMuted
                    } else {
                        EagleBlue
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (consulting) onCancelCall() else onDismiss()
                }
            ) {
                Text(
                    if (consulting) "Cancelar nova chamada" else "Cancelar",
                    color = EagleTextMuted
                )
            }
        },
        containerColor = EagleNavyLight
    )
}

@Composable
private fun AudioOutputDialog(
    outputs: List<SipAudioOutput>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Saída de áudio", color = EagleText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Escolha onde deseja ouvir a chamada.",
                    color = EagleTextMuted,
                    fontSize = 13.sp
                )
                if (outputs.isEmpty()) {
                    Text(
                        text = "Nenhuma saída disponível.",
                        color = EagleTextMuted,
                        fontSize = 13.sp
                    )
                } else {
                    outputs.forEach { output ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(13.dp))
                                .border(1.dp, EagleBorder, RoundedCornerShape(13.dp))
                                .clickable { onSelect(output.id) }
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = output.label,
                                color = EagleText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (output.selected) "●" else "○",
                                color = if (output.selected) EagleSuccess else EagleTextMuted,
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar", color = EagleBlue)
            }
        },
        containerColor = EagleNavyLight
    )
}

@Composable
private fun IncomingCallDialog(
    call: IncomingSipCall,
    contacts: List<EagleContact>,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val digits = call.number.filter(Char::isDigit)
    val contact = contacts.firstOrNull { item ->
        item.numbers.any { number ->
            val candidate = number.number.filter(Char::isDigit)
            candidate == digits ||
                (digits.isNotBlank() && candidate.endsWith(digits)) ||
                (candidate.isNotBlank() && digits.endsWith(candidate))
        }
    }
    val name = contact?.name
        ?: call.displayName?.takeUnless { it == call.number }
        ?: "Chamada recebida"

    AlertDialog(
        onDismissRequest = {},
        containerColor = EagleNavyLight,
        titleContentColor = EagleText,
        textContentColor = EagleTextMuted,
        title = {
            Text(
                text = "Chamada recebida",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ContactAvatar(
                    contact ?: EagleContact(
                        name = name,
                        numbers = emptyList(),
                        photo = null
                    )
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = name,
                    color = EagleText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = call.number,
                    color = EagleTextMuted,
                    fontSize = 16.sp
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onReject,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EagleDanger,
                    contentColor = EagleText
                )
            ) {
                Text("Recusar", fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EagleSuccess,
                    contentColor = EagleNavy
                )
            ) {
                Text("Atender", fontWeight = FontWeight.Bold)
            }
        }
    )
}

private data class DialKey(val digit: String, val letters: String = "")

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialKeyButton(
    key: DialKey,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(15.dp))
            .border(1.dp, EagleBorder, RoundedCornerShape(15.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = key.digit,
            color = EagleText,
            fontSize = 25.sp,
            lineHeight = 25.sp
        )
        if (key.letters.isNotEmpty()) {
            Text(
                text = key.letters,
                color = EagleText,
                fontSize = 9.sp,
                lineHeight = 9.sp
            )
        }
    }
}

@Composable
private fun DialActionButton(
    symbol: String,
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    danger: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val background = when {
        danger -> EagleDanger
        primary -> EagleBlueDark
        else -> EagleNavyLight
    }
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(background)
            .border(
                1.dp,
                if (primary || danger) EagleBlue else EagleBorder,
                RoundedCornerShape(15.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = symbol,
            color = if (danger) EagleText else if (enabled || primary) EagleBlue else EagleBorder,
            fontSize = if (primary) 25.sp else 19.sp,
            fontWeight = FontWeight.Bold
        )
        if (label.isNotEmpty()) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                color = if (enabled) EagleText else EagleTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AccountDialog(
    user: AuthenticatedUser,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Minha conta",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = user.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ramal ${user.extension}",
                    color = EagleTextMuted
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = user.email,
                    color = EagleTextMuted,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Sessão protegida pelo Android Keystore.",
                    color = EagleSuccess,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        },
        dismissButton = {
            TextButton(onClick = onLogout) {
                Text("Sair", color = EagleDanger)
            }
        },
        containerColor = EagleNavyLight,
        textContentColor = EagleText,
        titleContentColor = EagleText
    )
}

@Composable
private fun PresenceMenuItem(
    label: String,
    value: String,
    onSelect: (String) -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = { onSelect(value) }
    )
}

private enum class MainSection(
    val title: String,
    val shortTitle: String,
    val description: String
) {
    DIALER(
        title = "Discador",
        shortTitle = "Discador",
        description = "A telefonia nativa será conectada ao motor SIP nesta área."
    ),
    CONTACTS(
        title = "Contatos",
        shortTitle = "Contatos",
        description = "A agenda corporativa autorizada ficará disponível nesta área."
    ),
    HISTORY(
        title = "Histórico",
        shortTitle = "Histórico",
        description = "As chamadas e gravações autorizadas ficarão disponíveis nesta área."
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    EaglePBXTheme {
        LoginScreen()
    }
}
