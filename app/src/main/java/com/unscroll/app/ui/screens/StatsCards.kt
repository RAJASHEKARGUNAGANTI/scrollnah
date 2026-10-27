@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.unscroll.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unscroll.app.data.AppPeriod
import com.unscroll.app.data.MascotTheme
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.data.WeekReport
import com.unscroll.app.data.WeekStats
import com.unscroll.app.ui.components.AppTile
import com.unscroll.app.ui.components.BackArrowIcon
import com.unscroll.app.ui.components.ClockIcon
import com.unscroll.app.ui.components.ForwardChevronIcon
import com.unscroll.app.ui.components.NeoButtonPrimary
import com.unscroll.app.ui.components.NeoCard
import com.unscroll.app.ui.components.NeoPill
import com.unscroll.app.ui.components.NeoProgressBar
import com.unscroll.app.ui.components.UpArrowIcon
import com.unscroll.app.ui.components.duration
import com.unscroll.app.ui.components.spokenDuration
import com.unscroll.app.ui.mascot.LocalMascot
import com.unscroll.app.ui.mascot.Mascot
import com.unscroll.app.ui.mascot.MascotLines
import com.unscroll.app.ui.theme.BorderWidth
import com.unscroll.app.ui.theme.ButtonShape
import com.unscroll.app.ui.theme.CardShapeLarge
import com.unscroll.app.ui.theme.CheekPink
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Neo
import com.unscroll.app.ui.theme.Orange
import com.unscroll.app.ui.theme.contentColorOn
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// The Stats screen's cards (WeeklyReportScreen.kt puts them in order).

@Composable
private fun CardTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.headlineSmall, modifier = modifier.semantics { heading() })
}

/** "Where the time went": each limited app this week (or on the day tapped in the chart). */
@Composable
internal fun AppsCard(report: WeekReport, selectedDay: Int?, onShowWeek: () -> Unit) {
    val dayName = selectedDay?.let { index ->
        if (index == report.thisWeek.lastIndex) "Today"
        else SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(report.thisWeek[index].dayStart))
    }
    val list = if (selectedDay == null) report.weekApps else report.dayApps.getOrElse(selectedDay) { emptyList() }
    NeoCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardTitle(if (dayName == null) "Where the time went" else "$dayName's apps", Modifier.weight(1f))
            if (selectedDay != null) ChoicePill("Whole week", selected = false, isChoice = false, onClick = onShowWeek)
        }
        if (list.isEmpty()) {
            Text(
                if (dayName == null) "No time on your limited apps this week. Nice." else "No time on your limited apps that day.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        list.forEach { app -> AppStatRow(app, report.appName(app.packageName)) }
    }
}

@Composable
private fun AppStatRow(app: AppPeriod, name: String) {
    val visits = when {
        app.opens <= 0 -> null
        app.opens == 1 -> "1 visit"
        else -> "${app.opens} visits"
    }
    val perVisit = app.minutesPerVisit?.let { m -> if (m < 1f) "under 1 min each" else "${m.roundToInt()} min each" }
    val detail = listOfNotNull(visits, perVisit).joinToString(" · ")
    val change = app.previousMinutes?.let { app.minutes - it }
    val spokenChange = change?.let {
        when {
            it < 0 -> ", ${spokenDuration(-it)} less than last week"
            it > 0 -> ", ${spokenDuration(it)} more than last week"
            else -> ", same as last week"
        }
    }.orEmpty()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clearAndSetSemantics {
                contentDescription = "$name: ${spokenDuration(app.minutes)}" +
                    (visits?.let { ", $it" } ?: "") + spokenChange
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppTile(app.packageName, name)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(name, fontWeight = FontWeight.Bold)
            if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(duration(app.minutes), fontWeight = FontWeight.Bold)
            if (change != null && change != 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Down = less than last week (good), up = more.
                    UpArrowIcon(size = 14.dp, modifier = if (change < 0) Modifier.rotate(180f) else Modifier)
                    Text(duration(kotlin.math.abs(change)), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** The mascot's verdict on the week, in its own words. */
@Composable
internal fun VerdictCard(report: WeekReport) {
    val theme = LocalMascot.current.theme
    val lines = MascotLines.weekVerdict(theme, report.verdictFacts)
    val mood = when {
        (report.vsLastWeekMinutes ?: 0) < 0 -> MoodLevel.THRIVING
        (report.vsLastWeekMinutes ?: 0) > 0 -> MoodLevel.OKAY
        else -> MoodLevel.HAPPY
    }
    NeoCard(shape = CardShapeLarge, backgroundColor = Lime, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Mascot(mood = mood, size = 80.dp, contentDescription = theme.mascotName)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeoPill("${theme.mascotName} · my verdict")
                lines.forEach { Text(it, style = MaterialTheme.typography.bodyLarge) }
            }
        }
    }
}

/** Opens the swipeable "week in review" cards. */
@Composable
internal fun ReviewBannerCard(onOpen: () -> Unit) {
    NeoCard(shape = CardShapeLarge, backgroundColor = Orange, contentPadding = PaddingValues(16.dp)) {
        CardTitle("Your week in review")
        Text(
            "A few quick cards to flip through. Save or share your favourite.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        NeoButtonPrimary("Let's see", onClick = onOpen, backgroundColor = Lime)
    }
}

/** When the scrolling happens: minutes per hour this week, the busiest hour and the first scroll of the day. */
@Composable
internal fun ScrollClockCard(report: WeekReport) {
    NeoCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ClockIcon()
            CardTitle("Your scroll o'clock", Modifier.padding(start = 8.dp))
        }
        val peak = report.peakHour
        if (peak == null) {
            Text(
                "No scrolling on your limited apps yet this week.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
            return@NeoCard
        }
        Spacer(Modifier.height(12.dp))
        HourBars(report.hours, peak)
        Text(
            "Busiest hour: ${WeekStats.hourRange(peak)}",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp)
        )
        report.firstUseMinute?.let {
            Text(
                "First scroll of the day: around ${WeekStats.clockTime(it)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/** 24 little bars (midnight to midnight); the busiest hour in [peakColor]. */
@Composable
internal fun HourBars(hours: IntArray, peak: Int, peakColor: androidx.compose.ui.graphics.Color = Orange) {
    val max = (hours.maxOrNull() ?: 1).coerceAtLeast(1)
    val barShape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
    Column(Modifier.clearAndSetSemantics {}) { // the "Busiest hour" line says it in words
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            hours.forEachIndexed { hour, minutes ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight((minutes.toFloat() / max).coerceIn(0.04f, 1f))
                        .background(if (hour == peak) peakColor else Lime, barShape)
                        .border(1.dp, Neo.colors.outline, barShape)
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            listOf(0, 6, 12, 18).forEach { hour ->
                Text(
                    WeekStats.clockTime(hour * 60).replace(":00", ""),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** How often the Pause screen and break reminders actually worked. */
@Composable
internal fun NudgesCard(report: WeekReport) {
    val name = LocalMascot.current.theme.mascotName
    NeoCard(contentPadding = PaddingValues(16.dp)) {
        CardTitle("Did the nudges work?")
        if (report.pauses == 0 && report.breaksShown == 0) {
            Text(
                "No Pause screens or break reminders this week.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
            return@NeoCard
        }
        if (report.pauses > 0) {
            val share = report.walkedAway.toFloat() / report.pauses
            Row(
                Modifier
                    .padding(top = 12.dp)
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically
            ) {
                Donut(share, size = 84.dp)
                Text(
                    "$name stepped in ${report.pauses} ${if (report.pauses == 1) "time" else "times"}. " +
                        "You said \"Nah, I'm good\" ${report.walkedAway} of them.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 14.dp)
                )
            }
        }
        if (report.breaksShown > 0) {
            Column(
                Modifier
                    .padding(top = 14.dp)
                    .semantics(mergeDescendants = true) {}
            ) {
                Text(
                    "Break reminders: you took ${report.breaksTaken} of ${report.breaksShown}" +
                        if (report.breaksSnoozed > 0) " (${report.breaksSnoozed} snoozed)" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                NeoProgressBar(
                    progress = report.breaksTaken.toFloat() / report.breaksShown,
                    color = Lime,
                    spoken = "${report.breaksTaken} of ${report.breaksShown} breaks taken"
                )
            }
        }
    }
}

/** A ring that fills up to [share] (0..1), with the percentage in the middle. */
@Composable
private fun Donut(share: Float, size: Dp) {
    val track = Neo.colors.background
    val outline = Neo.colors.outline
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = this.size.minDimension * 0.16f
            val inset = stroke / 2f + 2.dp.toPx()
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(Lime, -90f, 360f * share.coerceIn(0f, 1f), false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            // Ink outlines inside and outside the ring (neo-brutalist border).
            val r = arcSize.minDimension / 2f
            drawCircle(outline, r + stroke / 2f, center, style = Stroke(2.dp.toPx()))
            drawCircle(outline, r - stroke / 2f, center, style = Stroke(2.dp.toPx()))
        }
        Text("${(share * 100).roundToInt()}%", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

/** A month of days: lime = under the limit, orange = over, plus Bloomy's new-flower days. */
@Composable
internal fun MonthCalendarCard(report: WeekReport) {
    val today = LocalDate.now()
    val thisMonth = YearMonth.from(today)
    val firstMonth = report.summaries.firstOrNull()?.let { YearMonth.from(LocalDate.parse(it.date)) } ?: thisMonth
    var shown by rememberSaveable { mutableStateOf(thisMonth.toString()) }
    val month = YearMonth.parse(shown)
    val showFlowers = LocalMascot.current.theme == MascotTheme.BLOOMY
    val cells = WeekStats.month(
        month, report.summaries, if (showFlowers) report.flowerDates else emptySet(),
        today, report.todayMinutes, report.todayLimit
    )
    val monthName = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

    NeoCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardTitle(monthName, Modifier.weight(1f))
            MonthButton(enabled = month > firstMonth, label = "Previous month", onClick = { shown = month.minusMonths(1).toString() }) {
                BackArrowIcon()
            }
            Spacer(Modifier.width(8.dp))
            MonthButton(enabled = month < thisMonth, label = "Next month", onClick = { shown = month.plusMonths(1).toString() }) {
                ForwardChevronIcon()
            }
        }
        Spacer(Modifier.height(10.dp))
        // Weekday letters, Monday first.
        Row(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
            java.time.DayOfWeek.entries.forEach { day ->
                Text(
                    day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { cell ->
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (cell != null) CalendarCell(cell.date, cell.minutes, cell.underLimit, cell.newFlower, cell.date == today)
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        // What the colours mean.
        FlowRow(
            Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LegendItem(Lime, "Under your limit")
            LegendItem(Orange, "Over")
            if (showFlowers) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FlowerDot()
                    Text("New flower", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun MonthButton(enabled: Boolean, label: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .clip(ButtonShape)
            .border(BorderWidth, Neo.colors.outline, ButtonShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) { icon() }
}

@Composable
private fun CalendarCell(date: LocalDate, minutes: Int?, underLimit: Boolean?, newFlower: Boolean, isToday: Boolean) {
    val shape = RoundedCornerShape(8.dp)
    val fill = when (underLimit) {
        true -> Lime
        false -> Orange
        null -> Neo.colors.background
    }
    val spokenDate = date.format(DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault()))
    val spoken = buildString {
        append(spokenDate)
        if (isToday) append(", today")
        when (underLimit) {
            true -> append(", ${spokenDuration(minutes ?: 0)}, under your limit")
            false -> append(", ${spokenDuration(minutes ?: 0)}, over your limit")
            null -> append(", no data")
        }
        if (newFlower) append(", a new flower grew")
    }
    Box(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(fill, shape)
            .border(if (isToday) 3.dp else if (underLimit == null) 1.dp else 2.dp, Neo.colors.outline.copy(alpha = if (underLimit == null && !isToday) 0.3f else 1f), shape)
            .clearAndSetSemantics { contentDescription = spoken },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "${date.dayOfMonth}",
            fontWeight = if (isToday) FontWeight.Black else FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            color = if (underLimit == null) Neo.colors.content.copy(alpha = 0.6f) else contentColorOn(fill)
        )
        if (newFlower) FlowerDot(Modifier.align(Alignment.TopEnd).padding(3.dp))
    }
}

/** A tiny pink flower (a dot with an ink outline and a yellow-ish centre). */
@Composable
private fun FlowerDot(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(10.dp)
            .background(CheekPink, CircleShape)
            .border(1.5.dp, Ink, CircleShape)
    )
}

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(14.dp)
                .background(color, RoundedCornerShape(4.dp))
                .border(1.5.dp, Neo.colors.outline, RoundedCornerShape(4.dp))
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 6.dp))
    }
}

/** Daily average for the last four 7-day stretches, to see the slow trend. */
@Composable
internal fun TrendCard(report: WeekReport) {
    val trend = report.trend
    val known = trend.mapNotNull { it.averageMinutes }
    NeoCard(contentPadding = PaddingValues(16.dp)) {
        CardTitle("4-week trend")
        val first = trend.firstOrNull { it.averageMinutes != null }?.averageMinutes
        val last = trend.last().averageMinutes
        Text(
            when {
                known.size < 2 || first == null || last == null -> "Daily average per week. Come back next week to see where it's heading."
                last < first -> "Your daily average went from ${duration(first)} to ${duration(last)}. Heading the right way."
                last > first -> "Your daily average went from ${duration(first)} to ${duration(last)}. Let's bring it back down."
                else -> "Your daily average is steady at ${duration(last)}."
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        val max = (known.maxOrNull() ?: 1).coerceAtLeast(1)
        val labels = listOf("3 weeks ago", "2 weeks ago", "Last week", "This week").takeLast(trend.size)
        val barShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
        // Three rows (bars, numbers, labels) instead of one fixed-height block, so big text
        // just makes the card taller and nothing gets cut off.
        Column(
            Modifier.clearAndSetSemantics {
                contentDescription = trend.indices.joinToString(". ") { i ->
                    "${labels[i]}: " + (trend[i].averageMinutes?.let { "${spokenDuration(it)} a day" } ?: "no data")
                }
            }
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                trend.forEachIndexed { i, week ->
                    val avg = week.averageMinutes
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(((avg ?: 0).toFloat() / max).coerceIn(0.04f, 1f))
                            .alpha(if (avg == null) 0.3f else 1f)
                            .background(if (i == trend.lastIndex) Neo.colors.content else Lime, barShape)
                            .border(2.dp, Neo.colors.outline, barShape)
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                trend.forEach { week ->
                    Text(
                        week.averageMinutes?.let { duration(it) } ?: "–",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                labels.forEach { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
