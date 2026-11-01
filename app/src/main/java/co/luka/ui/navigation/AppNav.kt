package co.luka.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import co.luka.domain.security.PinHasher
import co.luka.ui.components.FloatingBottomBar
import co.luka.ui.components.lukaDestinations
import co.luka.ui.home.HomeScreen
import co.luka.ui.lock.LockScreen
import co.luka.ui.lock.LockViewModel
import co.luka.ui.lock.PinSetupScreen
import co.luka.ui.movements.MovementsScreen
import co.luka.ui.onboarding.OnboardingScreen
import co.luka.ui.planning.BudgetsScreen
import co.luka.ui.planning.GoalsScreen
import co.luka.ui.planning.RecurringScreen
import co.luka.ui.settings.SettingsScreen
import co.luka.ui.settings.SettingsViewModel
import co.luka.ui.stats.StatsScreen
import co.luka.ui.theme.LukaTheme
import co.luka.ui.wallets.WalletsScreen

private fun routeFor(key: String) = when (key) {
    "movements" -> "movements?categoryId=-1"
    else -> key
}

@Composable
fun LukaRoot() {
    val activity = LocalContext.current as ComponentActivity
    val settingsViewModel: SettingsViewModel = hiltViewModel(activity)
    val lockViewModel: LockViewModel = hiltViewModel(activity)
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LukaTheme(settings.theme) {
        val pinReady = PinHasher.isConfigured(settings.pinSalt, settings.pinHash)
        when {
            !pinReady -> PinSetupScreen(
                onSave = { pin -> settingsViewModel.setPin(pin, pin, lockViewModel::unlock) },
            )
            !lockViewModel.unlocked -> LockScreen(
                pinHash = settings.pinHash,
                pinSalt = settings.pinSalt,
                onUnlock = lockViewModel::unlock,
                onPromptVisible = lockViewModel::holdPrompt,
            )
            !settings.onboardingDone -> OnboardingScreen()
            else -> AppScaffold()
        }
    }
}

@Composable
private fun AppScaffold() {
    val nav = rememberNavController()
    val destinations = lukaDestinations()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty()
    val selectedKey = when {
        route.startsWith("movements") -> "movements"
        route.startsWith("stats") -> "stats"
        route.startsWith("wallets") -> "wallets"
        route.startsWith("settings") -> "settings"
        else -> "home"
    }
    val showBar = route.startsWith("home") ||
        route.startsWith("movements") ||
        route.startsWith("stats") ||
        route.startsWith("wallets") ||
        route.startsWith("settings")
    val insets = if (showBar) {
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    } else {
        WindowInsets.safeDrawing
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = insets,
        bottomBar = {
            if (showBar) {
                FloatingBottomBar(
                    destinations = destinations,
                    selectedKey = selectedKey,
                    onSelect = { destination ->
                        nav.navigate(routeFor(destination.key)) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") {
                HomeScreen(
                    onBudgets = { nav.navigate("budgets") },
                    onGoals = { nav.navigate("goals") },
                    onRecurring = { nav.navigate("recurring") },
                    onMovements = {
                        nav.navigate("movements?categoryId=-1") {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(
                "movements?categoryId={categoryId}",
                arguments = listOf(navArgument("categoryId") { type = NavType.LongType; defaultValue = -1L }),
            ) { backStack ->
                val categoryId = backStack.arguments?.getLong("categoryId") ?: -1L
                MovementsScreen(initialCategoryId = categoryId.takeIf { it >= 0 })
            }
            composable("stats") {
                StatsScreen(onCategory = { id -> nav.navigate("movements?categoryId=$id") })
            }
            composable("wallets") { WalletsScreen() }
            composable("settings") {
                SettingsScreen(
                    onBudgets = { nav.navigate("budgets") },
                    onGoals = { nav.navigate("goals") },
                    onRecurring = { nav.navigate("recurring") },
                )
            }
            composable("budgets") { BudgetsScreen() }
            composable("goals") { GoalsScreen() }
            composable("recurring") { RecurringScreen() }
        }
    }
}
