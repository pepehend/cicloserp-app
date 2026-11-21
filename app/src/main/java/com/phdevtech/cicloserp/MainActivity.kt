package com.phdevtech.cicloserp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phdevtech.cicloserp.data.AuthState
import com.phdevtech.cicloserp.ui.LoggedInApp
import com.phdevtech.cicloserp.ui.common.LoadingBox
import com.phdevtech.cicloserp.ui.common.repository
import com.phdevtech.cicloserp.ui.login.LoginScreen
import com.phdevtech.cicloserp.ui.theme.CiclosERPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CiclosERPTheme { Root() }
        }
    }
}

@Composable
private fun Root() {
    val repo = repository()
    val auth by repo.auth.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (repo.auth.value is AuthState.Loading) repo.restore() }
    when (val a = auth) {
        AuthState.Loading -> LoadingBox()
        is AuthState.LoggedOut -> LoginScreen(a.serverUrl, a.message)
        is AuthState.LoggedIn -> LoggedInApp(a)
    }
}
