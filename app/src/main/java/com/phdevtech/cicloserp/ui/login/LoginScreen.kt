package com.phdevtech.cicloserp.ui.login

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.phdevtech.cicloserp.data.api.ApiClient
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phdevtech.cicloserp.data.CiclosRepository
import com.phdevtech.cicloserp.ui.common.appViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class LoginViewModel(private val repo: CiclosRepository) : ViewModel() {
    data class UiState(val busy: Boolean = false, val error: String? = null)

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    fun login(server: String, email: String, password: String) {
        if (server.isBlank() || email.isBlank() || password.isEmpty()) {
            _state.value = UiState(error = "Preencha o servidor, o e-mail e a senha.")
            return
        }
        _state.value = UiState(busy = true)
        viewModelScope.launch {
            _state.value = try {
                repo.login(server, email, password)
                UiState()
            } catch (e: Exception) {
                UiState(error = e.message ?: "Não foi possível entrar")
            }
        }
    }
}

@Composable
fun LoginScreen(initialServer: String?, notice: String?) {
    val vm = appViewModel { LoginViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()
    var server by rememberSaveable { mutableStateOf(initialServer?.removeSuffix("/") ?: DEFAULT_SERVER) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val ctx = LocalContext.current
    // Android 17+: servidor na rede local (ex.: 192.168.0.10) exige a permissão de rede local; internet pública não
    val lanPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.login(server, email, password) }
    val submit = {
        val host = ApiClient.normalizeBaseUrl(server)?.host
        val needsLan = host != null && ApiClient.isLocalNetworkHost(host) &&
            ContextCompat.checkSelfPermission(ctx, LAN_PERMISSION) != PackageManager.PERMISSION_GRANTED
        if (needsLan) lanPermission.launch(LAN_PERMISSION) else vm.login(server, email, password)
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
                Text("CiclosERP", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Ordens de serviço no celular", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(28.dp))
                OutlinedTextField(
                    value = server, onValueChange = { server = it },
                    label = { Text("Endereço do sistema") },
                    supportingText = { Text("O mesmo endereço que você abre no navegador") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("E-mail") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Senha") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showPassword) "Esconder senha" else "Mostrar senha")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                val msg = state.error ?: notice
                if (msg != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(20.dp))
                Button(onClick = submit, enabled = !state.busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    if (state.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Text("Entrar")
                }
            }
        }
    }
}

/** Manifest.permission.ACCESS_LOCAL_NETWORK (API 37). Em versões antigas o pedido é ignorado e a rede local já funciona. */
private const val LAN_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

/** Servidor de testes (pode ser trocado na tela). */
private const val DEFAULT_SERVER = "http://190.102.41.159"
