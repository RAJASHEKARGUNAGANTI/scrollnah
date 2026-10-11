package com.unscroll.app.ui.screens

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unscroll.app.appContainer
import com.unscroll.app.data.DayTotal
import com.unscroll.app.data.StreakInfo
import com.unscroll.app.data.UsageRepository
import com.unscroll.app.data.Websites
import com.unscroll.app.data.db.ReasonCount
import com.unscroll.app.ui.components.FireIcon
import com.unscroll.app.ui.components.MovieIcon
import com.unscroll.app.ui.components.NeoCard
import com.unscroll.app.ui.components.NeoPill
import com.unscroll.app.ui.components.NeoProgressBar
import com.unscroll.app.ui.components.duration
import com.unscroll.app.ui.components.formatMinutes
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Lilac
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Neo
import com.unscroll.app.ui.theme.Orange
import com.unscroll.app.ui.theme.ShadowOffsetLarge
import com.unscroll.app.ui.theme.White
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Everything the report shows, worked out from real usage history. */
private data class WeekReport(
    val thisWeek: List<DayTotal>,
    val totalMinutes: Int,
    val vsLastWeekMinutes: Int?, // negative = less than last week; null = not enough history
    val streakDays: Int,
    val bestStreak: Int,
    val dailyLimit: Int,
    val reasons: List<ReasonCount>, // Pause-screen answers, last 7 days
    val pauses: Int,
    val walkedAway: Int
)

@Composable
fun WeeklyReportScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val container = appContainer()
    val apps by container.database.trackedAppDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val hasAccess = remember { container.usage.hasUsageAccess() }
    var report by remember { mutableStateOf<WeekReport?>(null) }

    LaunchedEffect(apps) {
        if (!hasAccess || apps.isEmpty()) return@LaunchedEffect
        val packages = apps.map { it.packageName }.toSet()
        // The dashed line is the sum of today's enabled limits.
        val dailyLimit = apps.filter { it.enabled }.sumOf { it.limitMinutes }
        report = withContext(Dispatchers.IO) {
            val days = container.streaks.dailyTotalsWithWeb(packages, days = 14) // app + website time
            container.streaks.recordFinishedDays()
            val streakInfo = container.streaks.currentInfo()

            val weekStart = UsageRepository.startOfDay(6)
            val pauseDao = container.database.pauseLogDao()
            buildReport(
                days, dailyLimit, streakInfo,
                reasons = pauseDao.reasonCounts(weekStart),
                pauses = pauseDao.pauseCount(weekStart),
                walkedAway = pauseDao.walkedAwayCount(weekStart)
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenTitle("Your week", onBack) }
        val current = report
        when {
            !hasAccess -> item { InfoCard("Turn on Usage access from the Home screen to see your week.") }
            apps.isEmpty() -> item { InfoCard("Add some apps in Limits and your week will show up here.") }
            current == null -> item { InfoCard("Crunching your numbers...") }
            else -> {
                item { WeekSummaryCard(current) }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTileCard(
                            icon = { FireIcon() },
                            value = "${current.streakDays} ${if (current.streakDays == 1) "day" else "days"}",
                            label = if (current.bestStreak > current.streakDays) "under your limit in a row (best ${current.bestStreak})" else "under your limit in a row",
                            backgroundColor = Orange,
                            modifier = Modifier.weight(1f)
                        )
                        val wonBack = current.vsLastWeekMinutes?.let { -it }
                        StatTileCard(
                            icon = { MovieIcon() },
                            value = when {
                                wonBack == null -> "Soon"
                                wonBack <= 0 -> "0m"
                                wonBack >= 120 -> "${wonBack / 120} ${if (wonBack / 120 == 1) "movie" else "movies"}"
                                else -> duration(wonBack)
                            },
                            label = if (wonBack == null) "needs 2 weeks of history" else "of time won back vs last week",
                            backgroundColor = Neo.colors.surface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item { ReasonsCard(current) }
                item {
                    NeoCard(backgroundColor = Lilac, contentPadding = PaddingValues(16.dp)) {
                        Text("Friend challenges", fontWeight = FontWeight.Bold)
                        Text("Coming soon: race a friend to scroll less.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun buildReport(
    days: List<DayTotal>,
    dailyLimit: Int,
    streak: StreakInfo,
    reasons: List<ReasonCount>,
    pauses: Int,
    walkedAway: Int
): WeekReport {
    val lastWeek = days.take(7)
    val thisWeek = days.takeLast(7)
    val total = thisWeek.sumOf { it.minutes }
    val vsLast = if (lastWeek.all { it.hasData }) total - lastWeek.sumOf { it.minutes } else null

    return WeekReport(thisWeek, total, vsLast, streak.current, streak.best, dailyLimit, reasons, pauses, walkedAway)
}

/** What the user answered on the Pause screen this week. */
@Composable
private fun ReasonsCard(report: WeekReport) {
    NeoCard(contentPadding = PaddingValues(16.dp)) {
        Text("Why you reached for it", style = MaterialTheme.typography.headlineSmall)
        if (report.pauses == 0) {
            Text(
                "No Pause screens this week. Either you're doing great, or nothing hit its limit yet.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
            return@NeoCard
        }
        Text(
            "Blobby stepped in ${report.pauses} ${if (report.pauses == 1) "time" else "times"}. " +
                "You said \"Nah, I'm good\" ${report.walkedAway} of them.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
        )
        val topCount = report.reasons.firstOrNull()?.count ?: 0
        report.reasons.take(4).forEach { r ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(r.reason, fontWeight = FontWeight.Bold, modifier = Modifier.width(150.dp))
                NeoProgressBar(
                    progress = if (topCount > 0) r.count.toFloat() / topCount else 0f,
                    color = Orange,
                    modifier = Modifier.weight(1f)
                )
                Text("${r.count}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
            }
        }
        if (report.reasons.isEmpty()) {
            Text("Tap a reason on the Pause screen and it'll show up here.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    NeoCard(contentPadding = PaddingValues(16.dp)) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun WeekSummaryCard(report: WeekReport) {
    NeoCard(shadowOffset = ShadowOffsetLarge, contentPadding = PaddingValues(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text("Social time, last 7 days", style = MaterialTheme.typography.labelSmall)
                Text(duration(report.totalMinutes), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp))
            }
            report.vsLastWeekMinutes?.let { diff ->
                NeoPill(if (diff <= 0) "${duration(-diff)} less" else "${duration(diff)} more")
            }
        }
        Spacer(Modifier.height(14.dp))
        WeekChart(report.thisWeek, report.dailyLimit)
    }
}

@Composable
private fun WeekChart(days: List<DayTotal>, limitMinutes: Int) {
    val maxBarHeight = 140.dp
    val maxValue = (days.maxOf { it.minutes }.coerceAtLeast(limitMinutes).coerceAtLeast(1) * 1.15f)
    val limitFraction = (limitMinutes / maxValue).coerceIn(0f, 1f)
    val dayLetter = SimpleDateFormat("EEEEE", Locale.getDefault())
    val barShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp)

    Column {
        Box(Modifier.fillMaxWidth().height(maxBarHeight)) {
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                days.forEachIndexed { index, day ->
                    val isToday = index == days.lastIndex
                    val fraction = (day.minutes / maxValue).coerceIn(0.03f, 1f)
                    val color: Color = when {
                        isToday -> Neo.colors.content
                        day.minutes > limitMinutes -> Orange
                        else -> Lime
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction)
                            .alpha(if (day.hasData) 1f else 0.3f) // no history for that day
                            .background(color, barShape)
                            .border(2.dp, Neo.colors.outline, barShape)
                    )
                }
            }
            if (limitMinutes > 0) {
                val lineColor = Neo.colors.outline
                Canvas(Modifier.fillMaxSize()) {
                    val y = size.height * (1f - limitFraction)
                    drawLine(
                        color = lineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
                // Label sits on the left with a white backing so tall bars can't hide it.
                Text(
                    "${formatMinutes(limitMinutes)} limit",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(y = maxBarHeight * (1f - limitFraction) - 20.dp)
                        .background(Neo.colors.surface, RoundedCornerShape(6.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEach { day ->
                Text(
                    dayLetter.format(Date(day.dayStart)),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun StatTileCard(
    icon: @Composable () -> Unit,
    value: String,
    label: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    NeoCard(modifier = modifier, backgroundColor = backgroundColor, contentPadding = PaddingValues(14.dp)) {
        icon()
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
