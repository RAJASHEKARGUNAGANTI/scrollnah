package com.unscroll.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unscroll.app.appContainer
import com.unscroll.app.data.DayTotal
import com.unscroll.app.data.WeekReport
import com.unscroll.app.data.loadWeekReport
import com.unscroll.app.ui.components.FireIcon
import com.unscroll.app.ui.components.MovieIcon
import com.unscroll.app.ui.components.NeoCard
import com.unscroll.app.ui.components.NeoPill
import com.unscroll.app.ui.components.NeoProgressBar
import com.unscroll.app.ui.components.duration
import com.unscroll.app.ui.components.formatMinutes
import com.unscroll.app.ui.components.spokenDuration
import com.unscroll.app.ui.theme.Lilac
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Neo
import com.unscroll.app.ui.theme.Orange
import com.unscroll.app.ui.theme.ShadowOffsetLarge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * "Your week": the weekly report. The cards themselves live in StatsCards.kt; the numbers
 * come from [loadWeekReport].
 */
@Composable
fun WeeklyReportScreen(onBack: () -> Unit = {}, onOpenReview: () -> Unit = {}) {
    val container = appContainer()
    val apps by container.database.trackedAppDao().observeAll().collectAsStateWithLifecycle(emptyList())
    val hasAccess = remember { container.usage.hasUsageAccess() }
    var report by remember { mutableStateOf<WeekReport?>(null) }
    // A tapped bar in the chart (0-6) shows that day's apps; null = the whole week.
    var selectedDay by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(apps) {
        if (!hasAccess || apps.isEmpty()) return@LaunchedEffect
        report = withContext(Dispatchers.IO) { loadWeekReport(container, apps) }
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
                item { WeekSummaryCard(current, selectedDay, onSelectDay = { selectedDay = it }) }
                item { AppsCard(current, selectedDay, onShowWeek = { selectedDay = null }) }
                item { VerdictCard(current) }
                item { ReviewBannerCard(onOpenReview) }
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
                item { ScrollClockCard(current) }
                item { NudgesCard(current) }
                item { ReasonsCard(current) }
                item { MonthCalendarCard(current) }
                item { TrendCard(current) }
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

/** What the user answered on the Pause screen this week. */
@Composable
private fun ReasonsCard(report: WeekReport) {
    NeoCard(contentPadding = PaddingValues(16.dp)) {
        Text("Why you reached for it", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        if (report.reasons.isEmpty()) {
            Text(
                if (report.pauses == 0) "No Pause screens this week. Either you're doing great, or nothing hit its limit yet."
                else "Tap a reason on the Pause screen and it'll show up here.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
            return@NeoCard
        }
        val topCount = report.reasons.first().count
        report.reasons.take(4).forEach { r ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shares the row with the bar instead of a fixed width, so it fits narrow phones.
                Text(r.reason, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(end = 8.dp))
                NeoProgressBar(
                    progress = if (topCount > 0) r.count.toFloat() / topCount else 0f,
                    color = Orange,
                    modifier = Modifier.weight(1f)
                )
                Text("${r.count}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
            }
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
private fun WeekSummaryCard(report: WeekReport, selectedDay: Int?, onSelectDay: (Int?) -> Unit) {
    NeoCard(shadowOffset = ShadowOffsetLarge, contentPadding = PaddingValues(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) { // the pill keeps its room on narrow phones
                Text("Social time, last 7 days", style = MaterialTheme.typography.labelSmall)
                Text(duration(report.totalMinutes), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 40.sp))
            }
            report.vsLastWeekMinutes?.let { diff ->
                NeoPill(if (diff <= 0) "${duration(-diff)} less" else "${duration(diff)} more")
            }
        }
        Spacer(Modifier.height(14.dp))
        WeekChart(report.thisWeek, report.dailyLimits, selectedDay, onSelectDay)
        Text(
            "Tap a day to see its apps.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun WeekChart(days: List<DayTotal>, limits: List<Int>, selected: Int?, onSelect: (Int?) -> Unit) {
    val maxBarHeight = 140.dp
    val maxValue = (days.maxOf { it.minutes }.coerceAtLeast(limits.max()).coerceAtLeast(1) * 1.15f)
    // The label shows the usual (weekday) limit; weekend days may have their own line height.
    val limitMinutes = limits.first()
    val limitFraction = (limitMinutes / maxValue).coerceIn(0f, 1f)
    val gap = 8.dp
    val dayLetter = SimpleDateFormat("EEEEE", Locale.getDefault())
    val dayName = SimpleDateFormat("EEEE", Locale.getDefault())
    val barShape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp)

    Column {
        Box(Modifier.fillMaxWidth().height(maxBarHeight)) {
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                days.forEachIndexed { index, day ->
                    val isToday = index == days.lastIndex
                    val isSelected = selected == index
                    val fraction = (day.minutes / maxValue).coerceIn(0.03f, 1f)
                    val color: Color = when {
                        isToday -> Neo.colors.content
                        day.minutes > limits[index] -> Orange // over that day's own limit
                        else -> Lime
                    }
                    val name = if (isToday) "Today" else dayName.format(Date(day.dayStart))
                    val spoken = when {
                        !day.hasData -> "$name: no data"
                        day.minutes > limits[index] -> "$name: ${spokenDuration(day.minutes)}, over the limit"
                        else -> "$name: ${spokenDuration(day.minutes)}"
                    }
                    // The whole column is the touch target (tiny bars are still easy to tap).
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(role = Role.Button, onClickLabel = "show this day's apps") {
                                onSelect(if (isSelected) null else index)
                            }
                            .semantics {
                                contentDescription = spoken
                                this.selected = isSelected
                            },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                // No history for that day, or another day is picked: faded.
                                .alpha(if (!day.hasData || (selected != null && !isSelected)) 0.35f else 1f)
                                .background(color, barShape)
                                .border(if (isSelected) 4.dp else 2.dp, Neo.colors.outline, barShape)
                        )
                    }
                }
            }
            if (limitMinutes > 0) {
                val lineColor = Neo.colors.outline
                Canvas(Modifier.fillMaxSize()) {
                    // One dashed piece per day, at that day's limit (a straight line if they're all the same).
                    val gapPx = gap.toPx()
                    val column = (size.width - gapPx * (days.size - 1)) / days.size
                    limits.forEachIndexed { i, limit ->
                        val y = size.height * (1f - (limit / maxValue).coerceIn(0f, 1f))
                        val start = (i * (column + gapPx) - gapPx / 2f).coerceAtLeast(0f)
                        val end = (i * (column + gapPx) + column + gapPx / 2f).coerceAtMost(size.width)
                        drawLine(
                            color = lineColor,
                            start = Offset(start, y),
                            end = Offset(end, y),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
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
                        .semantics { contentDescription = "Daily limit ${spokenDuration(limitMinutes)}" }
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {}, // the bars already say their day
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            days.forEachIndexed { index, day ->
                Text(
                    dayLetter.format(Date(day.dayStart)),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = if (selected == index) FontWeight.Black else FontWeight.Bold,
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
    NeoCard(
        modifier = modifier.semantics(mergeDescendants = true) {}, // read as one: "5 days, under your limit in a row"
        backgroundColor = backgroundColor,
        contentPadding = PaddingValues(14.dp)
    ) {
        icon()
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
