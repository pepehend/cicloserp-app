package com.phdevtech.cicloserp.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.phdevtech.cicloserp.data.AuthState
import com.phdevtech.cicloserp.data.model.Me
import com.phdevtech.cicloserp.ui.agenda.AgendaScreen
import com.phdevtech.cicloserp.ui.common.repository
import com.phdevtech.cicloserp.ui.home.HomeScreen
import com.phdevtech.cicloserp.ui.order.OrderScreen
import com.phdevtech.cicloserp.ui.order.PhotosScreen
import com.phdevtech.cicloserp.ui.order.QuestionnaireScreen
import com.phdevtech.cicloserp.ui.order.ReportScreen
import com.phdevtech.cicloserp.ui.orders.OrdersScreen
import kotlinx.coroutines.launch

private object Route {
    const val HOME = "home"
    const val AGENDA = "agenda"
    const val ORDERS = "ordens"
    const val ACCOUNT = "conta"
    const val ORDER = "os/{id}"
    const val QUESTIONNAIRE = "os/{id}/questionario/{qid}"
    const val REPORT = "os/{id}/relatorio"
    const val PHOTOS = "os/{id}/fotos"
    fun order(id: Int) = "os/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Route.HOME, "Início", Icons.Outlined.Home),
    Tab(Route.AGENDA, "Agenda", Icons.Outlined.CalendarMonth),
    Tab(Route.ORDERS, "Ordens", Icons.AutoMirrored.Outlined.Assignment),
    Tab(Route.ACCOUNT, "Conta", Icons.Outlined.Person),
)

/** Navegação do app logado: abas (Início, Agenda, Ordens, Conta) e a ficha da OS com suas telas. */
@Composable
fun LoggedInApp(state: AuthState.LoggedIn) {
    val nav = rememberNavController()
    val me = state.me
    NavHost(nav, startDestination = Route.HOME) {
        composable(Route.HOME) { Tabs(nav, Route.HOME) { pad -> HomeScreen(me, { nav.navigate(Route.order(it)) }, pad) } }
        composable(Route.AGENDA) { Tabs(nav, Route.AGENDA) { pad -> AgendaScreen(me, { nav.navigate(Route.order(it)) }, pad) } }
        composable(Route.ORDERS) { Tabs(nav, Route.ORDERS) { pad -> OrdersScreen({ nav.navigate(Route.order(it)) }, pad) } }
        composable(Route.ACCOUNT) { Tabs(nav, Route.ACCOUNT) { pad -> AccountScreen(state, pad) } }
        composable(Route.ORDER, listOf(navArgument("id") { type = NavType.IntType })) { e ->
            val id = e.arguments!!.getInt("id")
            OrderScreen(
                me, id, onBack = { nav.popBackStack() },
                onQuestionnaire = { nav.navigate("os/$id/questionario/$it") },
                onReport = { nav.navigate("os/$id/relatorio") },
                onPhotos = { nav.navigate("os/$id/fotos") },
            )
        }
        composable(Route.QUESTIONNAIRE, listOf(navArgument("id") { type = NavType.IntType }, navArgument("qid") { type = NavType.IntType })) { e ->
            QuestionnaireScreen(me, e.arguments!!.getInt("id"), e.arguments!!.getInt("qid"), onBack = { nav.popBackStack() })
        }
        composable(Route.REPORT, listOf(navArgument("id") { type = NavType.IntType })) { e ->
            ReportScreen(me, e.arguments!!.getInt("id"), onBack = { nav.popBackStack() })
        }
        composable(Route.PHOTOS, listOf(navArgument("id") { type = NavType.IntType })) { e ->
            PhotosScreen(me, e.arguments!!.getInt("id"), onBack = { nav.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Tabs(nav: NavHostController, current: String, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(TABS.first { it.route == current }.label) }) },
        bottomBar = {
            NavigationBar {
                val route = nav.currentBackStackEntryAsState().value?.destination?.route
                TABS.forEach { t ->
                    NavigationBarItem(
                        selected = route == t.route,
                        onClick = {
                            if (route != t.route) nav.navigate(t.route) {
                                popUpTo(Route.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(t.icon, null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { pad -> content(pad) }
}

@Composable
private fun AccountScreen(state: AuthState.LoggedIn, pad: PaddingValues) {
    val repo = repository()
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    val me: Me = state.me
    Column(Modifier.fillMaxSize().padding(pad).padding(16.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(me.name, style = MaterialTheme.typography.titleLarge)
                Text(me.email, style = MaterialTheme.typography.bodyMedium)
                me.phone?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Text(if (me.isAdmin) "Administrador" else me.profileName ?: "Usuário", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
