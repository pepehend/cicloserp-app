package com.phdevtech.cicloserp.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phdevtech.cicloserp.data.AuthState
import com.phdevtech.cicloserp.ui.common.repository
import kotlinx.coroutines.launch

/** Minha conta: dados do usuário, servidor e saída (apaga o token deste aparelho no servidor). */
@Composable
fun AccountScreen(state: AuthState.LoggedIn, pad: PaddingValues) {
    val repo = repository()
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    val me = state.me
    Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(me.name, style = MaterialTheme.typography.titleLarge)
                Text(me.email, style = MaterialTheme.typography.bodyMedium)
                me.companyName?.let { Text("Empresa: $it", style = MaterialTheme.typography.bodyMedium) }
                me.phone?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Text(if (me.isAdmin) "Administrador" else me.profileName ?: "Usuário", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val ctx = androidx.compose.ui.platform.LocalContext.current
                val appVersion = remember {
                    runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "?"
                }
                Text("Aplicativo: versão $appVersion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Servidor: ${state.serverUrl}${state.serverVersion?.let { " (v$it)" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Button(onClick = { confirm = true }, modifier = Modifier.padding(top = 16.dp).fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Outlined.Logout, null); Text("  Sair")
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("Sair da conta?") },
        text = { Text("Este aparelho vai pedir login de novo.") },
        confirmButton = { TextButton(onClick = { confirm = false; scope.launch { repo.logout() } }) { Text("Sair") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } },
    )
}
