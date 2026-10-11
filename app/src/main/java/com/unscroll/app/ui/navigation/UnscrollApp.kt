package com.unscroll.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.unscroll.app.ui.screens.AddAppScreen
import com.unscroll.app.ui.screens.HomeScreen
import com.unscroll.app.ui.screens.LimitsScreen
import com.unscroll.app.ui.screens.PauseRoute
import com.unscroll.app.ui.screens.SettingsScreen
import com.unscroll.app.ui.screens.WeeklyReportScreen

@Composable
fun UnscrollApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // The "Add an app" screen isn't a tab, so highlight Limits while it's open.
    val currentDestination = when (currentRoute) {
        ADD_APP_ROUTE -> Destination.LIMITS
        else -> Destination.entries.find { it.route == currentRoute } ?: Destination.HOME
    }

    // While Limits has unsaved changes, switching tabs asks first (the prompt lives in LimitsScreen).
    var limitsUnsaved by remember { mutableStateOf(false) }
    var pendingLeave by remember { mutableStateOf<(() -> Unit)?>(null) }

    Scaffold(
        bottomBar = {
            if (currentDestination != Destination.PAUSE) {
                UnscrollBottomNav(current = currentDestination) { destination ->
                    val go = {
                        pendingLeave = null
                        navController.navigate(destination.route) {
                            popUpTo(Destination.HOME.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    if (currentRoute == Destination.LIMITS.route && limitsUnsaved && destination != Destination.LIMITS) {
                        pendingLeave = go
                    } else {
                        go()
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.HOME.route) {
                HomeScreen(
                    onEditLimits = { navController.navigate(Destination.LIMITS.route) },
                    onOpenSettings = { navController.navigate(SETTINGS_ROUTE) }
                )
            }
            composable(Destination.LIMITS.route) {
                LimitsScreen(
                    onBack = { navController.popBackStack() },
                    onAddApp = { navController.navigate(ADD_APP_ROUTE) },
                    onSaved = { navController.popBackStack() },
                    onUnsavedChanges = { limitsUnsaved = it },
                    pendingLeave = pendingLeave,
                    onLeaveCancelled = { pendingLeave = null }
                )
            }
            composable(SETTINGS_ROUTE) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(ADD_APP_ROUTE) {
                AddAppScreen(onBack = { navController.popBackStack() })
            }
            composable(Destination.STATS.route) {
                WeeklyReportScreen(onBack = { navController.popBackStack() })
            }
            composable(Destination.PAUSE.route) {
                PauseRoute(
                    onDone = { navController.popBackStack(Destination.HOME.route, inclusive = false) },
                    onGoToLimits = { navController.navigate(Destination.LIMITS.route) }
                )
            }
        }
    }
}
