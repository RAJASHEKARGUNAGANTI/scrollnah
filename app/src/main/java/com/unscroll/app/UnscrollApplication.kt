package com.unscroll.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.unscroll.app.data.DemoMode
import com.unscroll.app.data.GrowthRepository
import com.unscroll.app.data.MascotTheme
import com.unscroll.app.data.SettingsRepository
import com.unscroll.app.data.StreakRepository
import com.unscroll.app.data.UsageRepository
import com.unscroll.app.data.db.UnscrollDatabase
import com.unscroll.app.service.Allowances
import com.unscroll.app.service.FocusRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Holds the one-per-app objects (database, repositories) so every screen
 * and the Accessibility service share the same instances.
 */
class AppContainer(context: Context) {
    val database = UnscrollDatabase.create(context)
    val settings = SettingsRepository(context)
    val usage = UsageRepository(context)
    val growth = GrowthRepository(database, settings)
    val streaks = StreakRepository(context, usage, database, growth)

    /** For small background saves that must finish even if the screen that started them closes. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * The chosen mascot and Bloomy's flowers, kept in memory so the service,
     * notifications and the bubble can read them instantly (DataStore is async).
     */
    @Volatile var mascot: MascotTheme = MascotTheme.BLOBBY
        private set
    @Volatile var flowers: Int = 0
        private set
    /** Good days toward Bloomy's next flower (drawn as a growing bud). */
    @Volatile var budDays: Int = 0
        private set

    init {
        appScope.launch { settings.mascotTheme.collect { mascot = it } }
        appScope.launch {
            settings.potProgress.collect {
                flowers = it.flowers
                budDays = it.daysTowardNext
            }
        }
    }
}

class UnscrollApplication : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        DemoMode.init(this) // debug builds only; see DemoMode
        if (DemoMode.active) container.appScope.launch { DemoMode.seed(container.database, container.settings) }
        Allowances.init(this)
        FocusRules.init(this)
    }
}

/** Shortcut for composables: `val container = appContainer()`. */
@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as UnscrollApplication).container
