package com.unscroll.app.ui.navigation

import com.unscroll.app.ui.screens.WeekReviewScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.unscroll.app.ui.screens.AddAppScreen
import com.unscroll.app.ui.screens.BreakRemindersScreen
import com.unscroll.app.ui.screens.GardenScreen
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
    // "Add an app" and "Break reminders" aren't tabs, so highlight Limits while they're open.
    val currentDestination = when (currentRoute) {
        ADD_APP_ROUTE, BREAKS_ROUTE -> Destination.LIMITS
        WEEK_REVIEW_ROUTE -> Destination.STATS
        else -> Destination.entries.find { it.route == currentRoute } ?: Destination.HOME
    }

    // While Limits has unsaved changes, switching tabs asks first (the prompt lives in LimitsScreen).
    var limitsUnsaved by remember { mutableStateOf(false) }
    var pendingLeave by remember { mutableStateOf<(() -> Unit)?>(null) }

    Scaffold(
        bottomBar = {
            // Pause and the week-in-review cards are full screen.
            if (currentDestination != Destination.PAUSE && currentRoute != WEEK_REVIEW_ROUTE) {
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
            // consumeWindowInsets: screens that pad for the system bars themselves (Pause)
            // don't add the same space twice.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(Destination.HOME.route) {
                ReadableWidth {
                    HomeScreen(
                        onEditLimits = { navController.navigate(Destination.LIMITS.route) },
                        onOpenSettings = { navController.navigate(SETTINGS_ROUTE) },
                        onOpenGarden = { navController.navigate(GARDEN_ROUTE) }
                    )
                }
            }
            composable(Destination.LIMITS.route) {
                ReadableWidth {
                    LimitsScreen(
                        onBack = { navController.popBackStack() },
                        onAddApp = { navController.navigate(ADD_APP_ROUTE) },
                        onOpenBreaks = { navController.navigate(BREAKS_ROUTE) },
                        onSaved = { navController.popBackStack() },
                        onUnsavedChanges = { limitsUnsaved = it },
                        pendingLeave = pendingLeave,
                        onLeaveCancelled = { pendingLeave = null }
                    )
                }
            }
            composable(SETTINGS_ROUTE) {
                ReadableWidth {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenGarden = { navController.navigate(GARDEN_ROUTE) }
                    )
                }
            }
            composable(GARDEN_ROUTE) {
                ReadableWidth {
                    GardenScreen(onBack = { navController.popBackStack() })
                }
            }
            composable(BREAKS_ROUTE) {
                ReadableWidth {
                    BreakRemindersScreen(onBack = { navController.popBackStack() })
                }
            }
            composable(ADD_APP_ROUTE) {
                ReadableWidth {
                    AddAppScreen(onBack = { navController.popBackStack() })
                }
            }
            composable(Destination.STATS.route) {
                ReadableWidth {
                    WeeklyReportScreen(
                        onBack = { navController.popBackStack() },
                        onOpenReview = { navController.navigate(WEEK_REVIEW_ROUTE) }
                    )
                }
            }
            composable(WEEK_REVIEW_ROUTE) {
                WeekReviewScreen(onClose = { navController.popBackStack() })
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

/**
 * On wide screens (a sideways phone, a tablet) content stays a comfortable reading
 * width in the middle instead of stretching edge to edge. On a normal phone it fills the width.
 */
@Composable
private fun ReadableWidth(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
        ) { content() }
    }
}
