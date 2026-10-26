package com.unscroll.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.unscroll.app.data.MascotTheme
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.ui.mascot.Mascot
import com.unscroll.app.ui.mascot.MascotBitmap
import com.unscroll.app.ui.mascot.MascotLook
import com.unscroll.app.ui.theme.AppTheme
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Neo
import com.unscroll.app.ui.theme.Orange

/**
 * Debug builds only: every mascot mood and growth stage on one screen, to check the art.
 * Open it with:
 *   adb shell am start -n com.unscroll.app/.debug.MascotGalleryActivity
 */
class MascotGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppTheme { Gallery() } }
    }
}

@Composable
private fun Gallery() {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(Neo.colors.background)
            .systemBarsPadding(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item { Text("Mascot gallery (debug)", style = MaterialTheme.typography.headlineSmall) }
        item { Label("Blobby: thriving, happy, okay, worried, melting") }
        item { MoodRow(MascotLook(MascotTheme.BLOBBY), Lime) }
        item { MoodRow(MascotLook(MascotTheme.BLOBBY), Orange, onOrange = true) }
        for (flowers in 0..4) {
            item { Label("Bloomy, $flowers ${if (flowers == 1) "flower" else "flowers"}") }
            item { MoodRow(MascotLook(MascotTheme.BLOOMY, flowers), Lime) }
        }
        item { Label("Bloomy on orange (Pause screen)") }
        item { MoodRow(MascotLook(MascotTheme.BLOOMY, 3), Orange, onOrange = true) }
        item { Label("Bitmaps: badge (notification / bubble) and widget") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (theme in MascotTheme.entries) {
                    for (mood in listOf(MoodLevel.WORRIED, MoodLevel.OVER)) {
                        val bitmap = MascotBitmap.render(theme, mood, 3, 200, badge = true)
                        Image(bitmap.asImageBitmap(), null, Modifier.size(72.dp))
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (theme in MascotTheme.entries) {
                    for (mood in listOf(MoodLevel.THRIVING, MoodLevel.OKAY)) {
                        val bitmap = MascotBitmap.render(theme, mood, 2, 200)
                        Image(bitmap.asImageBitmap(), null, Modifier.size(72.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
}

/** The five moods side by side on a coloured strip. */
@Composable
private fun MoodRow(look: MascotLook, color: Color, onOrange: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(color),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MoodLevel.entries.forEach { mood ->
            Column { Mascot(mood = mood, look = look, size = 72.dp, onOrange = onOrange, animated = false) }
        }
    }
}
