package com.eaglesistemas.eaglepbx

import android.graphics.BitmapFactory
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
import androidx.lifecycle.viewmodel.compose.viewModel
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
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EaglePBXTheme {
                EaglePBXApp()
            }
        }
    }
}

@Composable
fun EaglePBXApp(viewModel: LoginViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    when {
        state.restoringSession -> LoadingScreen()
        state.user != null -> AuthenticatedScreen(
            user = requireNotNull(state.user),
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
            onToggleRecording = viewModel::toggleRecording,
            onSeekRecording = viewModel::seekRecording,
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
    onToggleRecording: (HistoryCall) -> Unit,
    onSeekRecording: (Int) -> Unit,
    onPresenceChange: (String) -> Unit,
    onLogout: () -> Unit
) {
    var presenceMenuOpen by remember { mutableStateOf(false) }
    var accountDialogOpen by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf(MainSection.DIALER) }
    val presenceLabel = when (user.presence) {
        "dnd" -> "Não perturbe"
        "offline" -> "Offline"
        else -> "Online"
    }
    val presenceColor = when (user.presence) {
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
                    MainSection.DIALER -> DialerContent()
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
private fun DialerContent() {
    var number by rememberSaveable { mutableStateOf("") }
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
        Text(
            text = number.ifBlank { "Digite o ramal ou telefone" },
            modifier = Modifier.weight(1f),
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
                    onClick = { number += key.digit },
                    onLongClick = {
                        if (key.digit == "0") {
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
            label = "Microfone",
            modifier = Modifier.weight(1f)
        )
        DialActionButton(
            symbol = "☎",
            label = "",
            primary = true,
            enabled = false,
            modifier = Modifier.weight(1f)
        )
        DialActionButton(
            symbol = "◖))",
            label = "Áudio",
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(7.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        DialActionButton("↦", "Transferir", enabled = false, modifier = Modifier.weight(1f))
        DialActionButton("Ⅱ", "Espera", enabled = false, modifier = Modifier.weight(1f))
        DialActionButton("☎+", "Adicionar", enabled = false, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Telefonia SIP em preparação",
        color = EagleTextMuted,
        fontSize = 11.sp
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
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (primary) EagleBlueDark else EagleNavyLight)
            .border(1.dp, if (primary) EagleBlue else EagleBorder, RoundedCornerShape(15.dp)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = symbol,
            color = if (enabled || primary) EagleBlue else EagleBorder,
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
