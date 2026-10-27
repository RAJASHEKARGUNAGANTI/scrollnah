package com.unscroll.app.ui.screens

import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unscroll.app.appContainer
import com.unscroll.app.data.Growth
import com.unscroll.app.data.MascotTheme
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.ui.components.NeoCard
import com.unscroll.app.ui.mascot.Mascot
import com.unscroll.app.ui.mascot.MascotLook
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Bloomy's garden: every pot the user filled with 4 flowers. Pots here stay forever. */
@Composable
fun GardenScreen(onBack: () -> Unit = {}) {
    val container = appContainer()
    val pots by container.database.gardenDao().observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val fullPot = MascotLook(MascotTheme.BLOOMY, Growth.MAX_FLOWERS)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenTitle("Your garden", onBack) }
        item {
            Text(
                "Each pot here took ${Growth.MAX_FLOWERS} weeks under your limits. They stay here forever.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        if (pots.isEmpty()) {
            item {
                NeoCard(contentPadding = PaddingValues(18.dp)) {
                    Mascot(
                        mood = MoodLevel.HAPPY,
                        look = MascotLook(MascotTheme.BLOOMY, 0),
                        animated = false,
                        size = 96.dp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Text("No full pots yet", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Stay under your limits ${Growth.DAYS_PER_FLOWER} days in a row to grow a flower. " +
                            "${Growth.MAX_FLOWERS} flowers fill a pot, and it moves here.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            // Two pots per row, newest first.
            val rows = pots.chunked(2)
            items(rows) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { pot ->
                        val number = pots.size - pots.indexOf(pot)
                        NeoCard(
                            modifier = Modifier
                                .weight(1f)
                                .semantics(mergeDescendants = true) {}, // "Pot 2, finished 3 Oct 2026"
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Mascot(mood = MoodLevel.THRIVING, look = fullPot, animated = false, size = 110.dp)
                                Text("Pot $number", fontWeight = FontWeight.Bold)
                                Text(
                                    "Finished ${formatDate(pot.completedOn)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f)) // keep a lone pot half-width
                }
            }
        }
    }
}

private fun formatDate(date: String): String =
    LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMM yyyy"))
