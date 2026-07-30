package com.eaglesistemas.eaglepbx.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eaglesistemas.eaglepbx.data.ApiException
import com.eaglesistemas.eaglepbx.data.AuthenticatedUser
import com.eaglesistemas.eaglepbx.data.EagleApiClient
import com.eaglesistemas.eaglepbx.data.SecureSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

data class LoginUiState(
    val restoringSession: Boolean = true,
    val submitting: Boolean = false,
    val user: AuthenticatedUser? = null,
    val error: String? = null
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val api = EagleApiClient(SecureSessionStore(application))
    private val mutableState = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { api.restoreSession() }
            }
            mutableState.value = LoginUiState(
                restoringSession = false,
                user = result.getOrNull(),
                error = result.exceptionOrNull()?.toFriendlyMessage()
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
        }
    }

    fun logout() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { api.logout() }
            mutableState.value = LoginUiState(restoringSession = false)
        }
    }

    private fun Throwable.toFriendlyMessage(): String = when (this) {
        is ApiException -> message ?: "Não foi possível entrar."
        is IOException -> "Não foi possível conectar ao Eagle PBX."
        else -> "O aplicativo não conseguiu concluir o acesso."
    }
}
