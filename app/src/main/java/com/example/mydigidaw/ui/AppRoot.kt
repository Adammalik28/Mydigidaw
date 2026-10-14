package com.example.mydigidaw.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mydigidaw.data.prefs.AppSettings
import com.example.mydigidaw.security.AuthResult
import com.example.mydigidaw.security.BiometricAuth
import com.example.mydigidaw.security.findActivity
import com.example.mydigidaw.ui.calendar.CalendarScreen
import com.example.mydigidaw.ui.calendar.EventEditorScreen
import com.example.mydigidaw.ui.components.AppFeedback
import com.example.mydigidaw.ui.components.AppNavBar
import com.example.mydigidaw.ui.components.NavBarItem
import com.example.mydigidaw.ui.components.LocalFeedback
import com.example.mydigidaw.ui.components.Motion
import com.example.mydigidaw.ui.finance.AccountsScreen
import com.example.mydigidaw.ui.finance.CategoriesScreen
import com.example.mydigidaw.ui.finance.DebtsScreen
import com.example.mydigidaw.ui.finance.FinanceScreen
import com.example.mydigidaw.ui.finance.GoalsScreen
import com.example.mydigidaw.ui.finance.RecurringScreen
import com.example.mydigidaw.ui.finance.TransactionEditorScreen
import com.example.mydigidaw.ui.focus.FocusScreen
import com.example.mydigidaw.ui.habits.HabitDetailScreen
import com.example.mydigidaw.ui.habits.HabitsScreen
import com.example.mydigidaw.ui.home.HomeScreen
import com.example.mydigidaw.ui.journal.JournalScreen
import com.example.mydigidaw.ui.lock.LockScreen
import com.example.mydigidaw.ui.more.MoreScreen
import com.example.mydigidaw.ui.navigation.Routes
import com.example.mydigidaw.ui.notes.NoteEditorScreen
import com.example.mydigidaw.ui.notes.NotesBinScreen
import com.example.mydigidaw.ui.notes.NotesScreen
import com.example.mydigidaw.ui.onboarding.OnboardingScreen
import com.example.mydigidaw.ui.search.SearchScreen
import com.example.mydigidaw.ui.settings.SettingsScreen
import com.example.mydigidaw.ui.tasks.TasksScreen
import com.example.mydigidaw.ui.vault.VaultItemEditorScreen
import java.time.LocalDate
import kotlinx.coroutines.launch

private data class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

private val topDestinations = listOf(
    TopDestination(Routes.HOME, "Beranda", Icons.Outlined.Home, Icons.Filled.Home),
    TopDestination(Routes.NOTES, "Catatan", Icons.AutoMirrored.Outlined.StickyNote2, Icons.AutoMirrored.Filled.StickyNote2),
    TopDestination(Routes.FINANCE, "Keuangan", Icons.Outlined.AccountBalanceWallet, Icons.Filled.AccountBalanceWallet),
    TopDestination(Routes.CALENDAR, "Kalender", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    TopDestination(Routes.MORE, "Lainnya", Icons.Outlined.GridView, Icons.Filled.GridView),
)

@Composable
fun AppRoot(
    settings: AppSettings,
    pendingRoute: String?,
    onRouteConsumed: () -> Unit,
) {
    val container = LocalAppContainer.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val locked by container.appLock.locked.collectAsStateWithLifecycle()

    val navigateTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val navigate: (String) -> Unit = { route ->
        if (route in Routes.topLevel) navigateTopLevel(route) else navController.navigate(route)
    }
    val back: () -> Unit = { navController.navigateUp() }

    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            navigate(pendingRoute)
            onRouteConsumed()
        }
    }

    val snackbarHost = remember { SnackbarHostState() }
    val feedbackScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val feedback = remember(snackbarHost, haptic) { AppFeedback(snackbarHost, feedbackScope, haptic) }
    var onboardingDone by remember { mutableStateOf(settings.onboarded) }

    CompositionLocalProvider(LocalFeedback provides feedback) {
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(snackbarHost) { data ->
                    Snackbar(
                        snackbarData = data,
                        shape = MaterialTheme.shapes.medium,
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        actionColor = MaterialTheme.colorScheme.inversePrimary,
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = currentRoute == null || currentRoute in Routes.topLevel,
                    enter = slideInVertically(tween(Motion.MEDIUM, easing = Motion.Emphasized)) { it } + fadeIn(),
                    exit = slideOutVertically(tween(Motion.SHORT)) { it } + fadeOut(tween(Motion.SHORT)),
                ) {
                    AppNavBar(
                        items = topDestinations.map { NavBarItem(it.label, it.icon, it.selectedIcon) },
                        selectedIndex = topDestinations.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0),
                        onSelect = { i ->
                            val dest = topDestinations[i]
                            if (currentRoute != dest.route) {
                                feedback.tick()
                                navigateTopLevel(dest.route)
                            }
                        },
                    )
                }
            },
            contentWindowInsets = WindowInsets(0),
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.padding(padding).consumeWindowInsets(padding),
                enterTransition = {
                    if (isTopLevelSwitch()) fadeIn(tween(Motion.MEDIUM)) + scaleIn(tween(Motion.MEDIUM), initialScale = 0.97f)
                    else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(Motion.MEDIUM, easing = Motion.Emphasized)) { it / 4 } + fadeIn(tween(Motion.MEDIUM))
                },
                exitTransition = {
                    if (isTopLevelSwitch()) fadeOut(tween(Motion.SHORT))
                    else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(Motion.MEDIUM, easing = Motion.Emphasized)) { it / 8 } + fadeOut(tween(Motion.MEDIUM))
                },
                popEnterTransition = {
                    if (isTopLevelSwitch()) fadeIn(tween(Motion.MEDIUM))
                    else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(Motion.MEDIUM, easing = Motion.Emphasized)) { it / 8 } + fadeIn(tween(Motion.MEDIUM))
                },
                popExitTransition = {
                    if (isTopLevelSwitch()) fadeOut(tween(Motion.SHORT))
                    else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(Motion.MEDIUM, easing = Motion.Emphasized)) { it / 4 } + fadeOut(tween(Motion.SHORT))
                },
            ) {
                composable(Routes.HOME) { HomeScreen(onNavigate = navigate) }
                composable(Routes.NOTES) { NotesScreen(onNavigate = navigate) }
                composable(Routes.FINANCE) { FinanceScreen(onNavigate = navigate) }
                composable(Routes.CALENDAR) { CalendarScreen(onNavigate = navigate) }
                composable(Routes.MORE) { MoreScreen(onNavigate = navigate) }

                composable(Routes.NOTE_EDIT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    NoteEditorScreen(noteId = entry.arguments?.getLong("id") ?: -1L, onBack = back)
                }
                composable(Routes.NOTE_BIN, arguments = listOf(navArgument("mode") { type = NavType.StringType })) { entry ->
                    NotesBinScreen(
                        mode = entry.arguments?.getString("mode") ?: "archive",
                        onBack = back,
                        onOpenNote = { navigate(Routes.noteEdit(it)) },
                    )
                }
                composable(
                    Routes.VAULT_EDIT,
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType },
                        navArgument("type") { type = NavType.StringType; nullable = true; defaultValue = null },
                    ),
                ) { entry ->
                    VaultItemEditorScreen(
                        itemId = entry.arguments?.getLong("id") ?: -1L,
                        initialType = entry.arguments?.getString("type"),
                        onBack = back,
                    )
                }
                composable(
                    Routes.TX_EDIT,
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType },
                        navArgument("type") { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("amount") { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("note") { type = NavType.StringType; nullable = true; defaultValue = null },
                    ),
                ) { entry ->
                    TransactionEditorScreen(
                        txId = entry.arguments?.getLong("id") ?: -1L,
                        initialType = entry.arguments?.getString("type"),
                        initialAmount = entry.arguments?.getString("amount")?.toLongOrNull(),
                        initialNote = entry.arguments?.getString("note"),
                        onBack = back,
                        onNavigate = navigate,
                    )
                }
                composable(Routes.ACCOUNTS) { AccountsScreen(onBack = back, onNavigate = navigate) }
                composable(Routes.CATEGORIES) { CategoriesScreen(onBack = back) }
                composable(Routes.GOALS) { GoalsScreen(onBack = back) }
                composable(Routes.DEBTS) { DebtsScreen(onBack = back) }
                composable(Routes.RECURRING) { RecurringScreen(onBack = back) }
                composable(
                    Routes.EVENT_EDIT,
                    arguments = listOf(
                        navArgument("id") { type = NavType.LongType },
                        navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                    ),
                ) { entry ->
                    val date = entry.arguments?.getString("date")?.toLongOrNull()?.let(LocalDate::ofEpochDay)
                    EventEditorScreen(eventId = entry.arguments?.getLong("id") ?: -1L, initialDate = date, onBack = back)
                }
                composable(
                    Routes.TASKS,
                    arguments = listOf(navArgument("new") { type = NavType.BoolType; defaultValue = false }),
                ) { entry ->
                    TasksScreen(openNew = entry.arguments?.getBoolean("new") ?: false, onBack = back)
                }
                composable(Routes.HABITS) { HabitsScreen(onBack = back, onOpenHabit = { navigate(Routes.habitDetail(it)) }) }
                composable(Routes.HABIT_DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                    HabitDetailScreen(habitId = entry.arguments?.getLong("id") ?: -1L, onBack = back)
                }
                composable(Routes.SETTINGS) { SettingsScreen(onBack = back) }
                composable(Routes.SEARCH) { SearchScreen(onBack = back, onNavigate = navigate) }
                composable(Routes.FOCUS) { FocusScreen(onBack = back) }
                composable(Routes.JOURNAL) { JournalScreen(onBack = back) }
            }
        }

        AnimatedVisibility(
            visible = !onboardingDone && !settings.onboarded && !locked,
            enter = fadeIn(),
            exit = fadeOut(tween(Motion.MEDIUM)) + scaleOut(tween(Motion.MEDIUM), targetScale = 1.08f),
        ) {
            OnboardingScreen(onFinished = { onboardingDone = true })
        }
        if (locked) AppLockOverlay()
    }
    }
}

@Composable
private fun AppLockOverlay() {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context.findActivity() as? FragmentActivity
    LockScreen(
        onUnlock = {
            scope.launch {
                if (!BiometricAuth.canUseDeviceAuth(context)) {
                    // Kunci layar perangkat dinonaktifkan: matikan kunci aplikasi agar tidak terkunci selamanya.
                    container.settings.setAppLock(false)
                    container.appLock.unlock()
                    Toast.makeText(context, "Kunci layar perangkat tidak aktif, kunci aplikasi dimatikan", Toast.LENGTH_LONG).show()
                    return@launch
                }
                if (activity == null) return@launch
                val result = container.appLock.withAuth {
                    BiometricAuth.authenticate(activity, "Buka Mydigidaw", "Gunakan sidik jari, wajah, atau PIN layar")
                }
                if (result is AuthResult.Success) container.appLock.unlock()
            }
        },
    )
}


private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTopLevelSwitch(): Boolean =
    initialState.destination.route in Routes.topLevel && targetState.destination.route in Routes.topLevel
