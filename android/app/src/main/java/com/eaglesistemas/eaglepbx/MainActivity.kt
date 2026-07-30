package com.eaglesistemas.eaglepbx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onLogout: () -> Unit
) {
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))
            Image(
                painter = painterResource(R.drawable.eagle_pbx_logo),
                contentDescription = "Eagle Sistemas",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = user.name,
                color = EagleText,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Ramal ${user.extension}",
                color = EagleTextMuted,
                fontSize = 17.sp
            )
            Spacer(Modifier.height(28.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(EagleNavyLight)
                    .border(1.dp, EagleBorder, RoundedCornerShape(22.dp))
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sessão",
                        color = EagleText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "CONECTADA",
                        color = EagleSuccess,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = user.email,
                    color = EagleTextMuted,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "A autenticação nativa está ativa. A telefonia será habilitada na próxima fase.",
                    color = EagleTextMuted,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EagleNavyLight,
                    contentColor = EagleText
                )
            ) {
                Text("Sair", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    EaglePBXTheme {
        LoginScreen()
    }
}
