package com.eaglesistemas.eaglepbx

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.eaglesistemas.eaglepbx.data.AuthenticatedUser
import com.eaglesistemas.eaglepbx.data.EagleContact
import com.eaglesistemas.eaglepbx.data.HistoryCall
import com.eaglesistemas.eaglepbx.ui.theme.EagleBlue
import com.eaglesistemas.eaglepbx.ui.theme.EagleBlueDark
import com.eaglesistemas.eaglepbx.ui.theme.EagleBorder
import com.eaglesistemas.eaglepbx.ui.theme.EagleDanger
import com.eaglesistemas.eaglepbx.ui.theme.EagleHeaderBorder
import com.eaglesistemas.eaglepbx.ui.theme.EagleHeaderNavy
import com.eaglesistemas.eaglepbx.ui.theme.EagleHeaderText
import com.eaglesistemas.eaglepbx.ui.theme.EagleHeaderTextMuted
import com.eaglesistemas.eaglepbx.ui.theme.EagleNavy
import com.eaglesistemas.eaglepbx.ui.theme.EagleNavyLight
import com.eaglesistemas.eaglepbx.ui.theme.EagleSuccess
import com.eaglesistemas.eaglepbx.ui.theme.EagleText
import com.eaglesistemas.eaglepbx.ui.theme.EagleTextMuted
import com.eaglesistemas.eaglepbx.ui.theme.EaglePBXTheme
import com.eaglesistemas.eaglepbx.ui.theme.EagleThemePreference
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
        if (
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1002)
        }
        handleNotificationAction(intent)
        observeLockedScreenCallEnd()
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        val themePreferences = getSharedPreferences("eagle-pbx-ui", MODE_PRIVATE)
        setContent {
            var themePreference by remember {
                mutableStateOf(
                    EagleThemePreference.fromStorage(
                        themePreferences.getString("theme", null)
                    )
                )
            }
            EaglePBXTheme(preference = themePreference) {
                EaglePBXApp(
                    viewModel = loginViewModel,
                    themePreference = themePreference,
                    onThemePreferenceChange = { preference ->
                        themePreference = preference
                        themePreferences.edit()
                            .putString("theme", preference.storageValue)
                            .apply()
                    }
                )
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
fun EaglePBXApp(
    viewModel: LoginViewModel = viewModel(),
    themePreference: EagleThemePreference = EagleThemePreference.SYSTEM,
    onThemePreferenceChange: (EagleThemePreference) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    when {
        state.restoringSession -> EaglePBXTheme(preference = EagleThemePreference.DARK) {
            LoadingScreen()
        }
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
            savingProfile = state.savingProfile,
            profileMessage = state.profileMessage,
            profileError = state.profileError,
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
            onUpdateProfile = viewModel::updateProfile,
            onClearProfileFeedback = viewModel::clearProfileFeedback,
            themePreference = themePreference,
            onThemePreferenceChange = onThemePreferenceChange,
            onLogout = viewModel::logout
        )
        else -> EaglePBXTheme(preference = EagleThemePreference.DARK) {
            LoginScreen(
                submitting = state.submitting,
                error = state.error,
                onLogin = viewModel::login
            )
        }
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
    savingProfile: Boolean,
    profileMessage: String?,
    profileError: String?,
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
    onUpdateProfile: (String, String, String?, ByteArray?, String?, String?) -> Unit,
    onClearProfileFeedback: () -> Unit,
    themePreference: EagleThemePreference,
    onThemePreferenceChange: (EagleThemePreference) -> Unit,
    onLogout: () -> Unit
) {
    var presenceMenuOpen by remember { mutableStateOf(false) }
    var accountDialogOpen by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf(MainSection.DIALER) }
    var externallyDialedNumber by rememberSaveable { mutableStateOf<String?>(null) }
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
    val userContact = remember(contacts, user.extension) {
        contacts.firstOrNull { contact ->
            contact.numbers.any { number ->
                number.number.filter(Char::isDigit) == user.extension.filter(Char::isDigit)
            }
        }
    }

    LaunchedEffect(selectedSection) {
        if (selectedSection == MainSection.CONTACTS) onLoadContacts(false)
        if (selectedSection == MainSection.HISTORY) onLoadHistory(false)
    }
    LaunchedEffect(user.extension) {
        onLoadContacts(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EagleHeaderNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.eagle_pbx_logo_official),
                    contentDescription = "Eagle Sistemas",
                    modifier = Modifier.size(68.dp),
                    colorFilter = ColorFilter.tint(EagleHeaderText)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = "eagle sistemas",
                        color = EagleHeaderText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "tecnologia e segurança",
                        color = EagleHeaderTextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box {
                    Row(
                        modifier = Modifier
                            .width(112.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, EagleHeaderBorder, RoundedCornerShape(10.dp))
                            .clickable(enabled = !updatingPresence) {
                                presenceMenuOpen = true
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (updatingPresence) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = EagleBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("●", color = presenceColor, fontSize = 12.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = presenceLabel,
                                color = EagleHeaderText,
                                fontSize = if (presenceLabel == "Não perturbe") 10.sp else 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Abrir estados de presença",
                                tint = EagleHeaderText,
                                modifier = Modifier.size(18.dp)
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
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 18.dp)
                    .background(EagleHeaderBorder.copy(alpha = 0.65f))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clickable { accountDialogOpen = true }
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(user = user)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 11.dp)
                ) {
                    Text(
                        text = user.name,
                        color = EagleHeaderText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ramal ${user.extension}",
                        color = EagleHeaderTextMuted,
                        fontSize = 13.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, EagleHeaderBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                onThemePreferenceChange(themePreference.next())
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (themePreference) {
                                EagleThemePreference.LIGHT -> Icons.Filled.LightMode
                                EagleThemePreference.DARK -> Icons.Filled.DarkMode
                                EagleThemePreference.SYSTEM -> Icons.Filled.DesktopWindows
                            },
                            contentDescription =
                                "Tema: ${themePreference.label}. Toque para alterar.",
                            tint = EagleHeaderText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, EagleHeaderBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("☰", color = EagleHeaderText, fontSize = 19.sp)
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
                    .then(
                        when (selectedSection) {
                            MainSection.DIALER -> Modifier
                                .background(EagleNavyLight)
                                .padding(horizontal = 16.dp)
                            MainSection.CONTACTS -> Modifier
                                .background(EagleNavyLight)
                            MainSection.HISTORY -> Modifier
                                .background(EagleNavyLight)
                        }
                    )
                    .padding(
                        horizontal = if (selectedSection == MainSection.DIALER) 0.dp else 22.dp,
                        vertical = if (selectedSection == MainSection.DIALER) 12.dp else 22.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (selectedSection) {
                    MainSection.DIALER -> DialerContent(
                        contacts = contacts,
                        externallyDialedNumber = externallyDialedNumber,
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
                        onRefresh = { onLoadContacts(true) },
                        onPlaceCall = { number ->
                            externallyDialedNumber = number
                            selectedSection = MainSection.DIALER
                            onPlaceCall(number)
                        }
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
                    .padding(horizontal = 6.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(EagleHeaderNavy)
                    .border(1.dp, EagleHeaderBorder, RoundedCornerShape(18.dp))
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
                                EagleHeaderTextMuted
                            }
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = when (section) {
                                    MainSection.DIALER -> Icons.Filled.Dialpad
                                    MainSection.CONTACTS -> Icons.Filled.Person
                                    MainSection.HISTORY -> Icons.Filled.History
                                },
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = section.shortTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
    if (accountDialogOpen) {
        AccountDialog(
            user = user,
            saving = savingProfile,
            message = profileMessage,
            error = profileError,
            onDismiss = { accountDialogOpen = false },
            onSave = onUpdateProfile,
            onClearFeedback = onClearProfileFeedback,
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
private fun UserAvatar(user: AuthenticatedUser) {
    val photo = user.avatar
    val bitmap = remember(photo) {
        val encoded = photo
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
            .size(56.dp)
            .clip(CircleShape)
            .background(EagleBlueDark)
            .border(1.dp, EagleHeaderTextMuted, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Foto de ${user.name}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = user.name
                    .split(' ')
                    .filter(String::isNotBlank)
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .joinToString("")
                    .ifBlank { "EP" },
                color = EagleText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
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
    onRefresh: () -> Unit,
    onPlaceCall: (String) -> Unit
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
                text = "AGENDA CORPORATIVA",
                color = EagleBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Contatos",
                color = EagleText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sua equipe em um só lugar.",
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
                    onClick = {
                        when (contact.numbers.size) {
                            1 -> onPlaceCall(contact.numbers.first().number)
                            in 2..Int.MAX_VALUE -> selectedContact = contact
                        }
                    }
                )
            }
        }
    }

    selectedContact?.let { contact ->
        ContactNumbersDialog(
            contact = contact,
            onDismiss = { selectedContact = null },
            onPlaceCall = { number ->
                selectedContact = null
                onPlaceCall(number)
            }
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
        ContactCallIcon(enabled = contact.numbers.isNotEmpty())
    }
}

@Composable
private fun ContactCallIcon(enabled: Boolean = true) {
    val iconColor = if (enabled) EagleSuccess else EagleTextMuted
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(iconColor.copy(alpha = 0.12f))
            .border(1.dp, iconColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_phone),
            contentDescription = if (enabled) "Ligar" else "Telefone indisponível",
            tint = iconColor,
            modifier = Modifier.size(21.dp)
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
    onDismiss: () -> Unit,
    onPlaceCall: (String) -> Unit
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
                                .clickable { onPlaceCall(entry.number) }
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
                            ContactCallIcon()
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
                text = "ATIVIDADE",
                color = EagleBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp
            )
            Spacer(Modifier.height(2.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val seekFraction = if (playerDuration > 0) {
        (position.toFloat() / playerDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
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
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active && playerDuration > 0) {
                                        EagleBlue
                                    } else {
                                        EagleTextMuted.copy(alpha = 0.58f)
                                    }
                                )
                        )
                    },
                    track = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(EagleTextMuted.copy(alpha = 0.22f))
                        ) {
                            if (seekFraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(seekFraction)
                                        .height(3.dp)
                                        .background(EagleBlue.copy(alpha = 0.82f))
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(18.dp)
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
    contacts: List<EagleContact>,
    externallyDialedNumber: String?,
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
    var callStartedAt by remember { mutableStateOf<Long?>(null) }
    var elapsedCallSeconds by remember { mutableStateOf(0) }
    var completedCallSeconds by remember { mutableStateOf<Int?>(null) }
    var completedTimerVisible by remember { mutableStateOf(false) }
    val dialToneGenerator = remember {
        runCatching {
            ToneGenerator(AudioManager.STREAM_DTMF, 70)
        }.getOrNull()
    }
    DisposableEffect(dialToneGenerator) {
        onDispose {
            dialToneGenerator?.release()
        }
    }
    LaunchedEffect(externallyDialedNumber) {
        externallyDialedNumber
            ?.takeIf(String::isNotBlank)
            ?.let { number = it }
    }
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
    LaunchedEffect(sipCallStatus) {
        when (sipCallStatus) {
            SipCallStatus.CONNECTED,
            SipCallStatus.HELD,
            SipCallStatus.ENDING -> {
                if (callStartedAt == null && sipCallStatus == SipCallStatus.CONNECTED) {
                    callStartedAt = SystemClock.elapsedRealtime()
                    elapsedCallSeconds = 0
                    completedCallSeconds = null
                    completedTimerVisible = false
                }
                while (callStartedAt != null) {
                    elapsedCallSeconds = (
                        (SystemClock.elapsedRealtime() - requireNotNull(callStartedAt)) / 1000L
                    ).toInt()
                    delay(250)
                }
            }
            SipCallStatus.IDLE,
            SipCallStatus.FAILED -> {
                val startedAt = callStartedAt
                if (startedAt != null) {
                    completedCallSeconds = (
                        (SystemClock.elapsedRealtime() - startedAt) / 1000L
                    ).toInt()
                    elapsedCallSeconds = completedCallSeconds ?: elapsedCallSeconds
                    callStartedAt = null
                    completedTimerVisible = true
                    delay(2000)
                    completedTimerVisible = false
                    delay(500)
                    completedCallSeconds = null
                }
            }
            else -> Unit
        }
    }
    val keys = listOf(
        DialKey("1"), DialKey("2", "ABC"), DialKey("3", "DEF"),
        DialKey("4", "GHI"), DialKey("5", "JKL"), DialKey("6", "MNO"),
        DialKey("7", "PQRS"), DialKey("8", "TUV"), DialKey("9", "WXYZ"),
        DialKey("*"), DialKey("0", "+"), DialKey("#")
    )
    val dialedDigits = number.filter(Char::isDigit)
    val matchedContact = remember(contacts, dialedDigits) {
        if (dialedDigits.isBlank()) {
            null
        } else {
            contacts.firstOrNull { contact ->
                contact.numbers.any { stored ->
                    stored.number.filter(Char::isDigit) == dialedDigits
                }
            }
        }
    }
    val matchedNumber = remember(matchedContact, dialedDigits) {
        matchedContact?.numbers?.firstOrNull { stored ->
            stored.number.filter(Char::isDigit) == dialedDigits
        }
    }

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
                fontSize = if (number.isBlank()) {
                    14.sp
                } else when {
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
    if (matchedContact != null) {
        Spacer(Modifier.height(7.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(EagleNavy)
                .border(1.dp, EagleBorder, RoundedCornerShape(16.dp))
                .clickable(
                    enabled = sipEngineStatus == SipEngineStatus.REGISTERED && !callActive
                ) { onPlaceCall(number) }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(matchedContact)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = matchedContact.name,
                    color = EagleText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = if (dialedDigits.length <= 5) {
                        "Ramal ${matchedNumber?.number.orEmpty()}"
                    } else {
                        matchedNumber?.label
                            ?.takeIf(String::isNotBlank)
                            ?.let { "$it ${matchedNumber.number}" }
                            ?: matchedNumber?.number.orEmpty()
                    },
                    color = EagleTextMuted,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            ContactCallIcon(
                enabled = sipEngineStatus == SipEngineStatus.REGISTERED && !callActive
            )
        }
    }
    Spacer(Modifier.height(7.dp))
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
                        dialToneGenerator?.startTone(
                            dialToneFor(key.digit.first()),
                            120
                        )
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
        Spacer(Modifier.height(6.dp))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        DialActionButton(
            icon = Icons.Filled.Mic,
            label = if (microphoneMuted) "Mudo" else "Microfone",
            enabled = sipCallStatus == SipCallStatus.CONNECTED,
            danger = microphoneMuted,
            onClick = onToggleMicrophone,
            modifier = Modifier.weight(1f)
        )
        DialActionButton(
            icon = Icons.Filled.Call,
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
            icon = Icons.Filled.VolumeUp,
            label = "Áudio",
            enabled = sipCallStatus == SipCallStatus.CONNECTED,
            onClick = {
                onLoadAudioOutputs()
                audioDialogOpen = true
            },
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        DialActionButton(
            icon = Icons.Filled.ArrowForward,
            label = "Transferir",
            enabled = sipCallStatus == SipCallStatus.CONNECTED,
            onClick = { transferDialogOpen = true },
            modifier = Modifier.weight(1f)
        )
        DialActionButton(
            icon = if (sipCallStatus == SipCallStatus.HELD) Icons.Filled.Call else Icons.Filled.Pause,
            label = if (sipCallStatus == SipCallStatus.HELD) "Retomar" else "Espera",
            enabled = sipCallStatus in setOf(SipCallStatus.CONNECTED, SipCallStatus.HELD),
            primary = sipCallStatus == SipCallStatus.HELD,
            onClick = onToggleCallHold,
            modifier = Modifier.weight(1f)
        )
        DialActionButton(
            icon = Icons.Filled.PersonAdd,
            label = "Adicionar",
            enabled = sipCallStatus == SipCallStatus.CONNECTED &&
                conferenceSetupStatus != ConferenceSetupStatus.ACTIVE,
            onClick = { addCallDialogOpen = true },
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(7.dp))
    val callStatusLabel = when {
        completedCallSeconds != null -> "Chamada encerrada"
        sipCallStatus == SipCallStatus.OUTGOING -> "Iniciando chamada..."
        sipCallStatus == SipCallStatus.INCOMING -> "Chamada recebida"
        sipCallStatus == SipCallStatus.RINGING -> "Chamando..."
        sipCallStatus == SipCallStatus.CONNECTED -> "Chamada em andamento"
        sipCallStatus == SipCallStatus.HELD -> "Chamada em espera"
        sipCallStatus == SipCallStatus.ENDING -> "Chamada em andamento"
        sipCallStatus == SipCallStatus.FAILED -> "Não foi possível completar a chamada"
        else -> ""
    }
    val callStatusShowsDuration = completedCallSeconds != null || sipCallStatus in setOf(
        SipCallStatus.CONNECTED,
        SipCallStatus.HELD,
        SipCallStatus.ENDING
    )
    AnimatedVisibility(
        visible = (
            sipCallStatus != SipCallStatus.IDLE && completedCallSeconds == null
        ) || completedTimerVisible,
        exit = fadeOut(animationSpec = tween(durationMillis = 500))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = callStatusLabel,
                color = if (
                    sipCallStatus == SipCallStatus.FAILED && !completedTimerVisible
                ) EagleDanger else EagleSuccess,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            if (callStatusShowsDuration) {
                Spacer(Modifier.width(10.dp))
                Text(
                    text = formatDuration(completedCallSeconds ?: elapsedCallSeconds),
                    color = EagleText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(3.dp))
    }
    val technicalError = when {
        sipEngineStatus == SipEngineStatus.REGISTRATION_FAILED -> "Telefone indisponível. Tentando reconectar..."
        sipEngineStatus == SipEngineStatus.UNAVAILABLE -> "Telefonia indisponível neste dispositivo."
        !mobileDeviceError.isNullOrBlank() -> "Não foi possível preparar a telefonia."
        else -> null
    }
    if (technicalError != null) {
        Text(
            text = technicalError,
            color = EagleDanger,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
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

private fun dialToneFor(digit: Char): Int = when (digit) {
    '0' -> ToneGenerator.TONE_DTMF_0
    '1' -> ToneGenerator.TONE_DTMF_1
    '2' -> ToneGenerator.TONE_DTMF_2
    '3' -> ToneGenerator.TONE_DTMF_3
    '4' -> ToneGenerator.TONE_DTMF_4
    '5' -> ToneGenerator.TONE_DTMF_5
    '6' -> ToneGenerator.TONE_DTMF_6
    '7' -> ToneGenerator.TONE_DTMF_7
    '8' -> ToneGenerator.TONE_DTMF_8
    '9' -> ToneGenerator.TONE_DTMF_9
    '*' -> ToneGenerator.TONE_DTMF_S
    '#' -> ToneGenerator.TONE_DTMF_P
    else -> ToneGenerator.TONE_DTMF_0
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
    val consulting = status in setOf(
        AttendedTransferStatus.CALLING,
        AttendedTransferStatus.CONNECTED,
        AttendedTransferStatus.COMPLETING
    )
    val canChooseTransfer = status in setOf(
        AttendedTransferStatus.IDLE,
        AttendedTransferStatus.FAILED
    )

    Dialog(
        onDismissRequest = {
            if (canChooseTransfer) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = canChooseTransfer,
            dismissOnClickOutside = canChooseTransfer,
            usePlatformDefaultWidth = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(EagleNavyLight)
                .border(1.dp, EagleBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "OPERAÇÃO TELEFÔNICA",
                        color = EagleBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (consulting) {
                            "Transferência assistida"
                        } else {
                            "Transferir chamada"
                        },
                        color = EagleText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = when (status) {
                            AttendedTransferStatus.CALLING ->
                                "Consultando $destination. A chamada original está em espera."
                            AttendedTransferStatus.CONNECTED ->
                                "Consulta estabelecida. Converse com o destino antes de concluir."
                            AttendedTransferStatus.COMPLETING ->
                                "Concluindo a transferência..."
                            else ->
                                "Escolha transferência direta ou consulte o destino antes de transferir."
                        },
                        color = EagleTextMuted,
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, EagleBorder, RoundedCornerShape(12.dp))
                        .clickable(enabled = status != AttendedTransferStatus.COMPLETING) {
                            if (consulting) onCancelAttended() else onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "×",
                        color = EagleText,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (!consulting) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Ramal ou telefone",
                    color = EagleText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
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
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (consulting) {
                    Button(
                        onClick = { onCancelAttended() },
                        enabled = status != AttendedTransferStatus.COMPLETING,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .border(1.dp, EagleBorder, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EagleNavyLight,
                            contentColor = EagleTextMuted
                        )
                    ) {
                        Text("Cancelar consulta", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            if (status == AttendedTransferStatus.CONNECTED) {
                                if (onCompleteAttended()) onDismiss()
                                else error = "Não foi possível concluir a transferência."
                            }
                        },
                        enabled = status == AttendedTransferStatus.CONNECTED,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EagleBlue,
                            contentColor = EagleText
                        )
                    ) {
                        Text(
                            if (status == AttendedTransferStatus.CALLING) {
                                "Aguardando..."
                            } else {
                                "Concluir transferência"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (destination.isNotBlank() && canChooseTransfer) {
                                if (onTransferDirect(destination)) onDismiss()
                                else error = "Não foi possível transferir a chamada."
                            }
                        },
                        enabled = destination.isNotBlank() && canChooseTransfer,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .border(1.dp, EagleBorder, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EagleNavyLight,
                            contentColor = EagleBlue
                        )
                    ) {
                        Text(
                            "Transferência direta",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = {
                            if (destination.isNotBlank() && canChooseTransfer &&
                                !onStartAttended(destination)
                            ) {
                                error = "Não foi possível iniciar a consulta."
                            }
                        },
                        enabled = destination.isNotBlank() && canChooseTransfer,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EagleBlue,
                            contentColor = EagleText
                        )
                    ) {
                        Text(
                            "Transferência assistida",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
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
    val canChooseParticipant = status in setOf(
        ConferenceSetupStatus.IDLE,
        ConferenceSetupStatus.FAILED
    )
    Dialog(
        onDismissRequest = {
            if (canChooseParticipant) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = canChooseParticipant,
            dismissOnClickOutside = canChooseParticipant,
            usePlatformDefaultWidth = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(EagleNavyLight)
                .border(1.dp, EagleBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "OPERAÇÃO TELEFÔNICA",
                        color = EagleBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (consulting) {
                            "Adicionar participante"
                        } else {
                            "Adicionar chamada"
                        },
                        color = EagleText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = when (status) {
                            ConferenceSetupStatus.CALLING ->
                                "Chamando $destination. O primeiro participante está em espera."
                            ConferenceSetupStatus.CONNECTED ->
                                "Segundo participante conectado. Forme a conferência ou cancele."
                            ConferenceSetupStatus.JOINING -> "Formando conferência..."
                            else ->
                                "O contato atual será mantido enquanto o novo participante atende."
                        },
                        color = EagleTextMuted,
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, EagleBorder, RoundedCornerShape(12.dp))
                        .clickable(enabled = status != ConferenceSetupStatus.JOINING) {
                            if (consulting) onCancelCall() else onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "×",
                        color = EagleText,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Ramal ou telefone",
                color = EagleText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .border(1.dp, EagleBorder, RoundedCornerShape(13.dp))
                        .padding(start = 12.dp, end = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = destination.ifBlank { "Ramal ou telefone" },
                        color = if (destination.isBlank()) EagleTextMuted else EagleText,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .border(1.dp, EagleBorder, RoundedCornerShape(11.dp))
                            .combinedClickable(
                                enabled = destination.isNotBlank() && !consulting,
                                onClick = { destination = destination.dropLast(1) },
                                onLongClick = { destination = "" }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⌫",
                            color = if (destination.isBlank()) EagleTextMuted else EagleText,
                            fontSize = 22.sp
                        )
                    }
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
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (consulting) onCancelCall() else onDismiss()
                    },
                    enabled = status != ConferenceSetupStatus.JOINING,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .border(1.dp, EagleBorder, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EagleNavyLight,
                        contentColor = EagleBlue
                    )
                ) {
                    Text(
                        if (consulting) "Cancelar nova chamada" else "Cancelar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                Button(
                    onClick = {
                        if (status == ConferenceSetupStatus.CONNECTED) {
                            if (onJoin()) onDismiss()
                            else error = "Não foi possível formar a conferência."
                        } else if (destination.isNotBlank() && canChooseParticipant) {
                            if (!onStart(destination)) {
                                error = "Não foi possível iniciar a nova chamada."
                            }
                        }
                    },
                    enabled = status == ConferenceSetupStatus.CONNECTED ||
                        (destination.isNotBlank() && canChooseParticipant),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EagleBlue,
                        contentColor = EagleText
                    )
                ) {
                    Text(
                        when (status) {
                            ConferenceSetupStatus.CONNECTED -> "Formar conferência"
                            ConferenceSetupStatus.CALLING -> "Aguardando atendimento..."
                            else -> "Adicionar à chamada"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioOutputDialog(
    outputs: List<SipAudioOutput>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(EagleNavyLight)
                .border(1.dp, EagleBorder, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ÁUDIO DA CHAMADA",
                        color = EagleBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Saída de áudio",
                        color = EagleText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Escolha onde deseja ouvir a chamada.",
                        color = EagleTextMuted,
                        fontSize = 13.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, EagleBorder, RoundedCornerShape(12.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "×",
                        color = EagleText,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (outputs.isEmpty()) {
                    Text(
                        text = "Nenhuma saída disponível.",
                        color = EagleTextMuted,
                        fontSize = 13.sp
                    )
                } else outputs.forEach { output ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, EagleBorder, RoundedCornerShape(14.dp))
                            .clickable { onSelect(output.id) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp)
                        ) {
                            Text(
                                text = output.label,
                                color = EagleText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (output.selected) "Saída selecionada" else "Selecionar saída",
                                color = EagleTextMuted,
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .border(
                                    2.dp,
                                    if (output.selected) EagleSuccess else EagleTextMuted,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (output.selected) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(EagleSuccess)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
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

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .background(EagleNavyLight)
                .border(3.dp, EagleBlue, RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(EagleBlue)
            )
            Text(
                text = "CHAMADA RECEBIDA",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 11.dp),
                color = EagleBlue,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(EagleBlue)
            )
            Spacer(Modifier.height(20.dp))
            ContactAvatar(
                contact ?: EagleContact(
                    name = name,
                    numbers = emptyList(),
                    photo = null
                )
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = name,
                color = EagleText,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = call.number,
                color = EagleTextMuted,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Chamando...",
                color = EagleBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onReject,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EagleDanger,
                        contentColor = EagleText
                    )
                ) {
                    Text("Recusar", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EagleSuccess,
                        contentColor = EagleNavy
                    )
                ) {
                    Text("Atender", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
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
            .height(50.dp)
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
    icon: ImageVector,
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
            .height(50.dp)
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (danger || primary) EagleText else if (enabled) EagleBlue else EagleBorder,
            modifier = Modifier.size(if (primary) 34.dp else 25.dp)
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
private fun LegacyAccountDialog(
    user: AuthenticatedUser,
    contact: EagleContact?,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationsEnabled by remember {
        mutableStateOf(areCallNotificationsEnabled(context))
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = areCallNotificationsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
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
                Spacer(Modifier.height(18.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(EagleHeaderNavy)
                        .border(1.dp, EagleBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notificações de chamada",
                                color = EagleText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Avisos do Android quando o aplicativo estiver em segundo plano.",
                                color = EagleTextMuted,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = if (notificationsEnabled) "Ativadas" else "Bloqueadas",
                            color = if (notificationsEnabled) EagleSuccess else EagleDanger,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (notificationsEnabled) {
                                        EagleSuccess.copy(alpha = 0.12f)
                                    } else {
                                        EagleDanger.copy(alpha = 0.12f)
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (notificationsEnabled) EagleSuccess else EagleDanger,
                                    RoundedCornerShape(9.dp)
                                )
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            if (notificationsEnabled) {
                                sendCallNotificationTest(context)
                            } else {
                                openAndroidNotificationSettings(context)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EagleNavyLight,
                            contentColor = EagleBlue
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EagleBlue)
                    ) {
                        Text(
                            text = if (notificationsEnabled) {
                                "Enviar notificação de teste"
                            } else {
                                "Abrir configurações do Android"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (notificationsEnabled) {
                            "As chamadas podem gerar avisos nativos do Eagle PBX no Android."
                        } else {
                            "Libere as notificações do Eagle PBX nas configurações do Android."
                        },
                        color = EagleTextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "Para receber chamadas em segundo plano, mantenha o Eagle PBX sem restrições de bateria.",
                        color = EagleTextMuted,
                        fontSize = 11.sp
                    )
                }
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
private fun AccountDialog(
    user: AuthenticatedUser,
    saving: Boolean,
    message: String?,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, ByteArray?, String?, String?) -> Unit,
    onClearFeedback: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var name by remember(user.id) { mutableStateOf(user.name) }
    var email by remember(user.id) { mutableStateOf(user.email) }
    var password by remember(user.id) { mutableStateOf("") }
    var avatarBytes by remember { mutableStateOf<ByteArray?>(null) }
    var avatarName by remember { mutableStateOf<String?>(null) }
    var avatarType by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    var notificationsEnabled by remember {
        mutableStateOf(areCallNotificationsEnabled(context))
    }
    val avatarBitmap = remember(avatarBytes) {
        avatarBytes?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val type = context.contentResolver.getType(uri).orEmpty()
                require(type in setOf("image/jpeg", "image/png", "image/webp")) {
                    "Escolha uma foto JPG, PNG ou WebP."
                }
                val bytes = context.contentResolver.openInputStream(uri)
                    ?.use { it.readBytes() }
                    ?: error("Não foi possível ler a foto selecionada.")
                require(bytes.size <= 5 * 1024 * 1024) {
                    "A foto deve ter no máximo 5 MB."
                }
                Triple(
                    bytes,
                    uri.lastPathSegment?.substringAfterLast('/') ?: "avatar",
                    type
                )
            }.onSuccess { selected ->
                avatarBytes = selected.first
                avatarName = selected.second
                avatarType = selected.third
                localError = null
            }.onFailure { failure ->
                localError = failure.message ?: "Não foi possível carregar a foto."
            }
        }
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = areCallNotificationsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(message) {
        if (message == "Perfil atualizado.") password = ""
    }

    Dialog(
        onDismissRequest = {
            if (!saving) {
                onClearFeedback()
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 18.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(EagleNavyLight)
                .border(1.dp, EagleBorder, RoundedCornerShape(26.dp))
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "MINHA CONTA",
                        color = EagleBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Perfil e segurança",
                        color = EagleText,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Atualize seus dados pessoais.",
                        color = EagleTextMuted,
                        fontSize = 14.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .border(1.dp, EagleBorder, RoundedCornerShape(13.dp))
                        .clickable(enabled = !saving) {
                            onClearFeedback()
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, "Fechar", tint = EagleText)
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EagleHeaderNavy)
                    .border(1.dp, EagleBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap,
                        contentDescription = "Nova foto do perfil",
                        modifier = Modifier.size(58.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    UserAvatar(user = user)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        name.ifBlank { user.name },
                        color = EagleText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Ramal ${user.extension}",
                        color = EagleTextMuted,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            AccountTextField(
                label = "Nome",
                value = name,
                enabled = !saving,
                onValueChange = { name = it; localError = null }
            )
            Spacer(Modifier.height(16.dp))
            AccountTextField(
                label = "E-mail",
                value = email,
                enabled = !saving,
                keyboardType = KeyboardType.Email,
                onValueChange = { email = it; localError = null }
            )
            Spacer(Modifier.height(16.dp))
            Text("Foto do perfil", color = EagleText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, EagleBorder, RoundedCornerShape(14.dp))
                    .clickable(enabled = !saving) { photoPicker.launch("image/*") },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxSize()
                        .background(EagleBlue.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Escolher foto", color = EagleBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    avatarName ?: "Nenhum arquivo selecionado",
                    color = EagleTextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.58f).padding(horizontal = 8.dp)
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                "JPG, PNG ou WebP, com até 5 MB. A imagem será ajustada ao avatar.",
                color = EagleTextMuted,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(16.dp))
            AccountTextField(
                label = "Nova senha",
                value = password,
                enabled = !saving,
                placeholder = "Deixe vazio para manter",
                password = true,
                keyboardType = KeyboardType.Password,
                onValueChange = { password = it; localError = null }
            )
            Spacer(Modifier.height(18.dp))
            AccountNotificationCard(
                enabled = notificationsEnabled,
                onAction = {
                    if (notificationsEnabled) sendCallNotificationTest(context)
                    else openAndroidNotificationSettings(context)
                }
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(EagleHeaderNavy)
                    .border(1.dp, EagleBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 15.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Versão do aplicativo", color = EagleTextMuted, fontSize = 12.sp)
                Text(BuildConfig.VERSION_NAME, color = EagleText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            val feedback = localError ?: error ?: message
            if (!feedback.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    feedback,
                    color = if (localError != null || error != null) EagleDanger else EagleSuccess,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    localError = when {
                        name.isBlank() -> "Informe o nome."
                        !email.contains('@') -> "Informe um e-mail válido."
                        password.isNotBlank() && password.length < 10 ->
                            "A senha deve ter ao menos 10 caracteres."
                        else -> null
                    }
                    if (localError == null) {
                        onSave(
                            name.trim(),
                            email.trim(),
                            password.takeIf(String::isNotBlank),
                            avatarBytes,
                            avatarName,
                            avatarType
                        )
                    }
                },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EagleBlue)
            ) {
                if (saving) CircularProgressIndicator(color = EagleText, modifier = Modifier.size(22.dp))
                else Text("Salvar", color = EagleText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onLogout,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EagleDanger.copy(alpha = 0.16f),
                    contentColor = EagleDanger
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, EagleDanger.copy(alpha = 0.65f))
            ) {
                Text("Logoff", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

}

@Composable
private fun AccountTextField(
    label: String,
    value: String,
    enabled: Boolean,
    placeholder: String? = null,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    Text(label, color = EagleText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(7.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        placeholder = if (placeholder == null) null else ({ Text(placeholder) }),
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = EagleNavyLight,
            unfocusedContainerColor = EagleNavyLight,
            focusedTextColor = EagleText,
            unfocusedTextColor = EagleText,
            focusedIndicatorColor = EagleBlue,
            unfocusedIndicatorColor = EagleBorder,
            cursorColor = EagleBlue,
            focusedPlaceholderColor = EagleTextMuted,
            unfocusedPlaceholderColor = EagleTextMuted
        ),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun AccountNotificationCard(enabled: Boolean, onAction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EagleHeaderNavy)
            .border(1.dp, EagleBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Notificações de chamada", color = EagleText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text("Avisos do Android quando o aplicativo estiver em segundo plano.", color = EagleTextMuted, fontSize = 12.sp)
            }
            Text(
                if (enabled) "Ativadas" else "Bloqueadas",
                color = if (enabled) EagleSuccess else EagleDanger,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background((if (enabled) EagleSuccess else EagleDanger).copy(alpha = 0.12f))
                    .border(1.dp, if (enabled) EagleSuccess else EagleDanger, RoundedCornerShape(9.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.height(13.dp))
        Button(
            onClick = onAction,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EagleNavyLight, contentColor = EagleBlue),
            border = androidx.compose.foundation.BorderStroke(1.dp, EagleBlue)
        ) {
            Text(
                if (enabled) "Enviar notificação de teste" else "Abrir configurações do Android",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(11.dp))
        Text(
            if (enabled) "As chamadas podem gerar avisos nativos do Eagle PBX no Android."
            else "Libere as notificações do Eagle PBX nas configurações do Android.",
            color = EagleTextMuted,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Mantenha o Eagle PBX sem restrições de bateria para receber chamadas em segundo plano.",
            color = EagleTextMuted,
            fontSize = 11.sp
        )
    }
}

private const val CALL_NOTIFICATION_CHANNEL_ID = "eagle_pbx_incoming_calls_v2"
private const val TEST_NOTIFICATION_ID = 1901

private fun areCallNotificationsEnabled(context: Context): Boolean {
    val runtimePermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    if (!runtimePermissionGranted ||
        !NotificationManagerCompat.from(context).areNotificationsEnabled()
    ) {
        return false
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CALL_NOTIFICATION_CHANNEL_ID)
        if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
            return false
        }
    }
    return true
}

private fun sendCallNotificationTest(context: Context) {
    if (!areCallNotificationsEnabled(context)) return
    val manager = context.getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(
            NotificationChannel(
                CALL_NOTIFICATION_CHANNEL_ID,
                "Chamadas recebidas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisa sobre novas chamadas do Eagle PBX."
                setSound(null, null)
                enableVibration(true)
            }
        )
    }
    val openApp = PendingIntent.getActivity(
        context,
        TEST_NOTIFICATION_ID,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, CALL_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Notificações ativadas")
        .setContentText("O Eagle PBX está pronto para avisar sobre novas chamadas.")
        .setContentIntent(openApp)
        .setAutoCancel(true)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()
    NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
}

private fun openAndroidNotificationSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
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
