package com.unscroll.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.unscroll.app.service.BlockerService
import com.unscroll.app.ui.navigation.UnscrollApp
import com.unscroll.app.ui.screens.OnboardingScreen
import com.unscroll.app.ui.theme.AppTheme
import com.unscroll.app.ui.theme.Neo
import com.unscroll.app.widget.MascotWidgets
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** True once we know whether to show onboarding or the app; the opening screen stays until then. */
    @Volatile private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // The opening screen (Blobby on lilac). Must come before super.onCreate.
        installSplashScreen().setKeepOnScreenCondition { !ready }
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                AppRoot(onReady = { ready = true })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Make sure the blocker is running (e.g. right after usage access was granted).
        BlockerService.startIfNeeded(this)
        // Keep the home-screen widget in sync with what the app shows.
        val container = (application as UnscrollApplication).container
        container.appScope.launch { MascotWidgets.refresh(this@MainActivity) }
    }
}

/** Shows the first-run walkthrough once, then the normal app. */
@Composable
private fun AppRoot(onReady: () -> Unit) {
    val container = appContainer()
    // null = still checking, true = show onboarding, false = show the app
    val needsOnboarding by produceState<Boolean?>(initialValue = null) {
        val done = container.settings.onboarded.first()
        value = when (done) {
            true -> false
            false -> true
            null -> {
                // Fresh flag. People who set up Unscroll before onboarding existed skip it.
                val hasApps = container.database.trackedAppDao().getAll().isNotEmpty()
                if (hasApps) container.settings.setOnboarded(true)
                !hasApps
            }
        }
    }
    var showApp by remember { mutableStateOf(false) }
    if (needsOnboarding != null) onReady() // the opening screen can go now

    when {
        needsOnboarding == null -> Box(Modifier.fillMaxSize().background(Neo.colors.background)) // hidden by the opening screen
        needsOnboarding == true && !showApp -> OnboardingScreen(onFinished = { showApp = true })
        else -> UnscrollApp()
    }
}
