package com.unscroll.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.unscroll.app.data.SettingsRepository
import com.unscroll.app.data.StreakRepository
import com.unscroll.app.data.UsageRepository
import com.unscroll.app.data.db.UnscrollDatabase
import com.unscroll.app.service.Allowances
import com.unscroll.app.service.FocusRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Holds the one-per-app objects (database, repositories) so every screen
 * and the Accessibility service share the same instances.
 */
class AppContainer(context: Context) {
    val database = UnscrollDatabase.create(context)
    val settings = SettingsRepository(context)
    val usage = UsageRepository(context)
    val streaks = StreakRepository(context, usage, database)

    /** For small background saves that must finish even if the screen that started them closes. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

class UnscrollApplication : Application() {
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        Allowances.init(this)
        FocusRules.init(this)
    }
}

/** Shortcut for composables: `val container = appContainer()`. */
@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as UnscrollApplication).container
