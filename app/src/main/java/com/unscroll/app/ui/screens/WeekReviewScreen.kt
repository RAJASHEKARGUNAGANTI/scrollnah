package com.unscroll.app.ui.screens

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.Dp
import android.content.ClipData
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unscroll.app.appContainer
import com.unscroll.app.data.MascotTheme
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.data.WeekReport
import com.unscroll.app.data.WeekStats
import com.unscroll.app.data.loadWeekReport
import com.unscroll.app.ui.components.AppTile
import com.unscroll.app.ui.components.CloseIcon
import com.unscroll.app.ui.components.NeoButtonPrimary
import com.unscroll.app.ui.components.NeoPill
import com.unscroll.app.ui.components.NeoShadowSurface
import com.unscroll.app.ui.components.duration
import com.unscroll.app.ui.mascot.LocalMascot
import com.unscroll.app.ui.mascot.Mascot
import com.unscroll.app.ui.mascot.MascotLines
import com.unscroll.app.ui.theme.Background
import com.unscroll.app.ui.theme.BorderWidth
import com.unscroll.app.ui.theme.ButtonShape
import com.unscroll.app.ui.theme.CardShapeLarge
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Lilac
import com.unscroll.app.ui.theme.LightNeoColors
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.LocalNeoColors
import com.unscroll.app.ui.theme.Neo
import com.unscroll.app.ui.theme.Orange
import com.unscroll.app.ui.theme.PillShape
import com.unscroll.app.ui.theme.White
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** True when the cards are short (small phone or big text): cards then say a little less, so nothing is cut off. */
private val LocalShortCard = compositionLocalOf { false }

/** One card in the week in review: its colour, what's on it, and the mascot's mood in the corner (null = no mascot). */
private class ReviewCard(val color: Color, val mascot: MoodLevel? = null, val content: @Composable ColumnScope.() -> Unit)

/**
 * "Your week in review": a few swipeable cards (like a year-in-review, but every week).
 * Each card can be saved as a picture and shared through Android's share menu.
 */
@Composable
fun WeekReviewScreen(onClose: () -> Unit) {
    val container = appContainer()
    val apps by container.database.trackedAppDao().observeAll().collectAsStateWithLifecycle(emptyList())
    var report by remember { mutableStateOf<WeekReport?>(null) }
    LaunchedEffect(apps) {
        if (apps.isEmpty() || !container.usage.hasUsageAccess()) return@LaunchedEffect
        report = withContext(Dispatchers.IO) { loadWeekReport(container, apps) }
    }

    // The cards always use the light colours, so a shared picture looks the same in dark mode.
    CompositionLocalProvider(LocalNeoColors provides LightNeoColors, LocalContentColor provides Ink) {
        Column(
            Modifier
                .fillMaxSize()
                .background(Background)
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            val current = report
            val cards = current?.let { reviewCards(it, LocalMascot.current.theme) }.orEmpty()
            val pager = rememberPagerState { cards.size }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Your week in review", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(ButtonShape)
                        .border(BorderWidth, Ink, ButtonShape)
                        .clickable(role = Role.Button, onClick = onClose)
                        .semantics { contentDescription = "Close" },
                    contentAlignment = Alignment.Center
                ) { CloseIcon() }
            }
            if (current == null) {
                Text("Crunching your numbers...", modifier = Modifier.padding(top = 16.dp))
                return@Column
            }
            // Page dots.
            Row(
                Modifier
                    .padding(vertical = 12.dp)
                    .semantics { contentDescription = "Card ${pager.currentPage + 1} of ${cards.size}" },
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                cards.indices.forEach { i ->
                    Box(
                        Modifier
                            .height(8.dp)
                            .width(if (i == pager.currentPage) 28.dp else 8.dp)
                            .background(if (i <= pager.currentPage) Orange else White, PillShape)
                            .border(BorderWidth, Ink, PillShape)
                    )
                }
            }
            // One picture recorder per card, so "Share" can save exactly what's on screen.
            val layers = cards.map { rememberGraphicsLayer() }
            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f),
                pageSpacing = 16.dp
            ) { page ->
                BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // As tall as the space allows (up to 460dp), so the whole card is on screen and in the picture.
                    val cardHeight = minOf(460.dp, maxHeight - 20.dp)
                    Box(
                        Modifier
                            .widthIn(max = 420.dp)
                            .drawWithContent {
                                layers[page].record { this@drawWithContent.drawContent() }
                                drawLayer(layers[page])
                            }
                            .background(Background) // the shared picture gets the lilac backdrop too
                            .padding(10.dp)
                    ) {
                        ReviewCardFrame(cards[page], cardHeight)
                    }
                }
            }
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            Spacer(Modifier.height(12.dp))
            NeoButtonPrimary(
                "Share this card",
                onClick = { scope.launch { shareCard(context, layers[pager.currentPage]) } },
                backgroundColor = Lime
            )
        }
    }
}

/** The cards that have something to say (a card with no data is left out). */
private fun reviewCards(report: WeekReport, theme: MascotTheme): List<ReviewCard> {
    val cards = mutableListOf<ReviewCard>()
    val mood = when {
        (report.vsLastWeekMinutes ?: 0) < 0 -> MoodLevel.THRIVING
        (report.vsLastWeekMinutes ?: 0) > 0 -> MoodLevel.OKAY
        else -> MoodLevel.HAPPY
    }

    // 1. The week's total.
    cards += ReviewCard(Lime, mascot = mood) {
        NeoPill("My week")
        BigNumber(duration(report.totalMinutes))
        Text("on my time-sinks in the last 7 days", style = MaterialTheme.typography.bodyLarge)
        report.vsLastWeekMinutes?.let { diff ->
            Spacer(Modifier.height(12.dp))
            NeoPill(if (diff <= 0) "${duration(-diff)} less than last week" else "${duration(diff)} more than last week")
        }
    }

    // 2. The top app.
    report.weekApps.firstOrNull()?.let { top ->
        val name = report.appName(top.packageName)
        val share = if (report.totalMinutes > 0) top.minutes * 100 / report.totalMinutes else 0
        cards += ReviewCard(White) {
            NeoPill("My top time-sink")
            Spacer(Modifier.height(20.dp))
            AppTile(top.packageName, name, size = 72.dp)
            Text(name, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 12.dp))
            BigNumber(duration(top.minutes))
            if (top.opens > 0) Text("opened ${top.opens} ${if (top.opens == 1) "time" else "times"}", style = MaterialTheme.typography.bodyLarge)
            if (share in 1..100) Text("That's $share% of my week.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
        }
    }

    // 3. Best and toughest day.
    val best = report.verdictFacts.bestDay
    val toughest = report.verdictFacts.toughestDay
    if (best != null && toughest != null && best.first != toughest.first) {
        val dayName = SimpleDateFormat("EEEE", Locale.getDefault())
        fun name(date: java.time.LocalDate) = dayName.format(Date(date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()))
        cards += ReviewCard(Lilac) {
            NeoPill("Best day")
            Text(name(best.first), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp))
            BigNumber(duration(best.second))
            Spacer(Modifier.height(28.dp))
            NeoPill("Toughest day")
            Text(name(toughest.first), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp))
            BigNumber(duration(toughest.second))
        }
    }

    // 4. Scroll o'clock.
    report.peakHour?.let { peak ->
        cards += ReviewCard(Orange) {
            NeoPill("My scroll o'clock")
            Spacer(Modifier.height(12.dp))
            BigNumber(WeekStats.hourRange(peak))
            Text("is when I scroll the most.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(20.dp))
            HourBars(report.hours, peak, peakColor = White) // orange-on-orange wouldn't show
            report.firstUseMinute?.let {
                Spacer(Modifier.height(20.dp))
                Text("My first scroll of the day: around ${WeekStats.clockTime(it)}", fontWeight = FontWeight.Bold)
            }
        }
    }

    // 5. The mascot's verdict.
    cards += ReviewCard(White, mascot = mood) {
        val short = LocalShortCard.current
        NeoPill("${theme.mascotName} · my verdict")
        Spacer(Modifier.height(12.dp))
        MascotLines.weekVerdict(theme, report.verdictFacts).take(if (short) 2 else 3).forEach {
            Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 10.dp))
        }
        if (report.streakDays > 0 && !short) {
            Text(
                "${report.streakDays}-day streak" +
                    if (theme == MascotTheme.BLOOMY) " · ${report.flowerDates.size} ${if (report.flowerDates.size == 1) "flower" else "flowers"} grown" else "",
                fontWeight = FontWeight.Bold
            )
        }
    }
    return cards
}

@Composable
private fun BigNumber(text: String) {
    Text(text, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 52.sp, lineHeight = 56.sp))
}

/** A card: brand colour, ink border, hard shadow and the "scrollnah" wordmark at the bottom. */
@Composable
private fun ReviewCardFrame(card: ReviewCard, height: Dp) {
    NeoShadowSurface(shape = CardShapeLarge, backgroundColor = card.color) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(height)
                .padding(22.dp)
        ) {
            // The wordmark always stays at the bottom; very big text scrolls inside the card instead.
            CompositionLocalProvider(LocalShortCard provides (height < 440.dp)) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    content = card.content
                )
            }
            // Bottom row: the wordmark, and the mascot in the corner (smaller on short screens).
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.Bottom) {
                Text("scrollnah", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                card.mascot?.let { Mascot(mood = it, size = if (height < 440.dp) 88.dp else 120.dp) }
            }
        }
    }
}

/** Saves the card as a picture and opens Android's share menu (nothing is uploaded by ScrollNah). */
private suspend fun shareCard(context: Context, layer: GraphicsLayer) {
    try {
        val picture = layer.toImageBitmap().asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
        val file = withContext(Dispatchers.IO) {
            File(context.cacheDir, "shared").apply { mkdirs() }.resolve("scrollnah-week.png").also { f ->
                f.outputStream().use { picture.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // Also as clip data, so the share menu itself may read the picture (for its preview).
        send.clipData = ClipData.newRawUri(null, uri)
        context.startActivity(Intent.createChooser(send, "Share your week"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to share with", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Couldn't make the picture. Try again?", Toast.LENGTH_SHORT).show()
    }
}
