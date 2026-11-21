package com.phdevtech.cicloserp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import com.phdevtech.cicloserp.ui.common.IconTile
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.phdevtech.cicloserp.data.AuthState
import com.phdevtech.cicloserp.data.model.Me
import com.phdevtech.cicloserp.ui.account.AccountScreen
import com.phdevtech.cicloserp.ui.agenda.AgendaScreen
import com.phdevtech.cicloserp.ui.common.repository
import com.phdevtech.cicloserp.ui.customers.CustomerScreen
import com.phdevtech.cicloserp.ui.customers.CustomersScreen
import com.phdevtech.cicloserp.ui.equipments.EquipmentsScreen
import com.phdevtech.cicloserp.ui.home.HomeScreen
import com.phdevtech.cicloserp.ui.order.OrderScreen
import com.phdevtech.cicloserp.ui.order.PhotosScreen
import com.phdevtech.cicloserp.ui.order.QuestionnaireScreen
import com.phdevtech.cicloserp.ui.order.ReportScreen
import com.phdevtech.cicloserp.ui.orders.OrdersScreen
import com.phdevtech.cicloserp.ui.products.ProductsScreen
import com.phdevtech.cicloserp.ui.quotes.QuoteScreen
import com.phdevtech.cicloserp.ui.quotes.QuotesScreen
import kotlinx.coroutines.launch

object Route {
    const val HOME = "home"
    const val AGENDA = "agenda"
    const val ORDERS = "ordens"
    const val CUSTOMERS = "clientes"
    const val QUOTES = "orcamentos"
    const val PRODUCTS = "produtos"
    const val SERVICES = "servicos"
    const val EQUIPMENTS = "equipamentos"
    const val ACCOUNT = "conta"
    const val CUSTOMER = "cliente/{id}"
    const val QUOTE = "orcamento/{id}"
    const val ORDER = "os/{id}"
    const val QUESTIONNAIRE = "os/{id}/questionario/{qid}"
    const val REPORT = "os/{id}/relatorio"
    const val PHOTOS = "os/{id}/fotos"
    fun order(id: Int) = "os/$id"
}

/** Item do menu lateral (igual ao app do técnico do sistema web). [perm] = permissão para aparecer. */
private data class Entry(val route: String, val label: String, val icon: ImageVector, val perm: String?)

private val MENU = listOf(
    Entry(Route.HOME, "Início", Icons.Outlined.Home, null),
    Entry(Route.AGENDA, "Agenda", Icons.Outlined.CalendarMonth, "ordens"),
    Entry(Route.ORDERS, "Ordens de serviço", Icons.AutoMirrored.Outlined.Assignment, "ordens"),
    Entry(Route.CUSTOMERS, "Clientes", Icons.Outlined.Group, "clientes"),
    Entry(Route.QUOTES, "Orçamentos", Icons.Outlined.Description, "orcamentos"),
    Entry(Route.PRODUCTS, "Produtos", Icons.Outlined.Inventory2, "produtos"),
    Entry(Route.SERVICES, "Serviços", Icons.Outlined.Build, "servicos"),
    Entry(Route.EQUIPMENTS, "Equipamentos", Icons.Outlined.Settings, "equipamentos"),
    Entry(Route.ACCOUNT, "Minha conta", Icons.Outlined.Person, null),
)

/** App logado: menu lateral com os módulos (conforme as permissões) e as telas de detalhe. */
@Composable
fun LoggedInApp(state: AuthState.LoggedIn) {
    val nav = rememberNavController()
    val me = state.me
    val entries = MENU.filter { it.perm == null || me.can(it.perm) }
    val go: (String) -> Unit = { route ->
        nav.navigate(route) {
            popUpTo(Route.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val openOrder: (Int) -> Unit = { nav.navigate(Route.order(it)) }

    NavHost(nav, startDestination = Route.HOME) {
        composable(Route.HOME) { Shell(nav, me, entries, Route.HOME, go) { pad -> HomeScreen(me, openOrder, pad, onNavigate = go, can = { me.can(it) }) } }
        composable(Route.AGENDA) { Shell(nav, me, entries, Route.AGENDA, go) { pad -> AgendaScreen(me, openOrder, pad) } }
        composable(Route.ORDERS) { Shell(nav, me, entries, Route.ORDERS, go) { pad -> OrdersScreen(openOrder, pad) } }
        composable(Route.CUSTOMERS) { Shell(nav, me, entries, Route.CUSTOMERS, go) { pad -> CustomersScreen({ nav.navigate("cliente/$it") }, pad) } }
        composable(Route.QUOTES) { Shell(nav, me, entries, Route.QUOTES, go) { pad -> QuotesScreen({ nav.navigate("orcamento/$it") }, pad) } }
        composable(Route.PRODUCTS) { Shell(nav, me, entries, Route.PRODUCTS, go) { pad -> ProductsScreen(service = false, pad = pad) } }
        composable(Route.SERVICES) { Shell(nav, me, entries, Route.SERVICES, go) { pad -> ProductsScreen(service = true, pad = pad) } }
        composable(Route.EQUIPMENTS) { Shell(nav, me, entries, Route.EQUIPMENTS, go) { pad -> EquipmentsScreen(pad) } }
        composable(Route.ACCOUNT) { Shell(nav, me, entries, Route.ACCOUNT, go) { pad -> AccountScreen(state, pad) } }
        composable(Route.CUSTOMER, listOf(navArgument("id") { type = NavType.IntType })) { e ->
            CustomerScreen(e.arguments!!.getInt("id"), onBack = { nav.popBackStack() })
        }
        composable(Route.QUOTE, listOf(navArgument("id") { type = NavType.IntType })) { e ->
            QuoteScreen(e.arguments!!.getInt("id"), onBack = { nav.popBackStack() })
        }
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

/** Tela de nível superior: barra com o botão do menu e a gaveta de navegação. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Shell(
    nav: androidx.navigation.NavHostController,
    me: Me,
    entries: List<Entry>,
    current: String,
    go: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val repo = repository()
    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) {
                // Cabeçalho vermelho com avatar, nome, perfil e Sair (igual ao app do técnico web)
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(initials(me.name), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(me.name, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        Text(if (me.isAdmin) "Administrador" else me.profileName ?: "Usuário", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
                    }
                    Column(
                        Modifier.clickable { scope.launch { drawer.close(); repo.logout() } }
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = MaterialTheme.colorScheme.onPrimary)
                        Text("Sair", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.padding(4.dp))
                entries.forEach { e ->
                    val selected = e.route == current
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { scope.launch { drawer.close() }; if (!selected) go(e.route) }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconTile(e.icon, MaterialTheme.colorScheme.primary, size = 36.dp)
                        Text(e.label, Modifier.padding(start = 14.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                // Só o botão do menu (e o atalho do início nas telas de módulo), sem título: o título fica na página
                TopAppBar(
                    title = {},
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = { IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Outlined.Menu, "Menu") } },
                    actions = { if (current != Route.HOME) IconButton(onClick = { go(Route.HOME) }) { Icon(Icons.Outlined.Home, "Início") } },
                )
            },
        ) { pad -> content(pad) }
    }
}
private fun initials(name: String): String =
    name.trim().split(' ').filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
