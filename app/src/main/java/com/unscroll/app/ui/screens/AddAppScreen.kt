package com.unscroll.app.ui.screens

import com.unscroll.app.ui.components.AppTile
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unscroll.app.appContainer
import com.unscroll.app.data.DEFAULT_APP_LIMIT_MINUTES
import com.unscroll.app.data.InstalledApp
import com.unscroll.app.data.db.TrackedApp
import com.unscroll.app.data.launchableApps
import com.unscroll.app.ui.components.AppSearchField
import com.unscroll.app.ui.components.LetterTile
import com.unscroll.app.ui.components.NeoCard
import com.unscroll.app.ui.components.PlusIcon
import com.unscroll.app.ui.components.letterFor
import com.unscroll.app.ui.components.duration
import com.unscroll.app.ui.components.tileColorFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Lists the apps on the phone (most used today first) so the user can add one to their limits. */
@Composable
fun AddAppScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val dao = container.database.trackedAppDao()

    val tracked by dao.observeAll().collectAsStateWithLifecycle(emptyList())
    var installed by remember { mutableStateOf<List<InstalledApp>?>(null) }
    var minutesToday by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            installed = launchableApps(context)
            if (container.usage.hasUsageAccess()) {
                minutesToday = container.usage.todayUsageMinutes(packages = null)
            }
        }
    }

    val trackedPackages = tracked.map { it.packageName }.toSet()
    val choices = installed.orEmpty()
        .filter { it.packageName !in trackedPackages }
        .filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }
        .sortedWith(compareByDescending<InstalledApp> { minutesToday[it.packageName] ?: 0 }.thenBy { it.label.lowercase() })

    Column(Modifier.fillMaxSize()) {
        // Title and search stay at the top while the list scrolls under them.
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenTitle("Add an app", onBack)
            AppSearchField(query = query, onQueryChange = { query = it })
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding(), // the last apps stay reachable above the keyboard
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                installed == null -> item { Text("Finding your apps...", style = MaterialTheme.typography.bodyLarge) }
                choices.isEmpty() -> item { Text("No apps match that.", style = MaterialTheme.typography.bodyLarge) }
            }
            items(choices, key = { it.packageName }) { app ->
                AppChoiceRow(
                    app = app,
                    minutesToday = minutesToday[app.packageName] ?: 0,
                    onClick = {
                        scope.launch {
                            dao.insert(TrackedApp(app.packageName, app.label, DEFAULT_APP_LIMIT_MINUTES))
                            onBack()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AppChoiceRow(app: InstalledApp, minutesToday: Int, onClick: () -> Unit) {
    NeoCard(
        modifier = Modifier.clickable(role = Role.Button, onClickLabel = "add", onClick = onClick),
        shadow = false,
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppTile(app.packageName, app.label)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(app.label, fontWeight = FontWeight.Bold)
                Text(
                    if (minutesToday > 0) "${duration(minutesToday)} today" else "Not used today",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            PlusIcon()
        }
    }
}
