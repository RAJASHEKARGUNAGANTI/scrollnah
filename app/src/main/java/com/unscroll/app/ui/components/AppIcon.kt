package com.unscroll.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.unscroll.app.ui.theme.BorderWidth
import com.unscroll.app.ui.theme.Neo
import com.unscroll.app.ui.theme.TileShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The app's real icon, read from the phone (so it adds nothing to ScrollNah's size),
 * in our rounded tile with an ink border. Shows the letter tile while loading, or if
 * the app isn't installed any more.
 */
@Composable
fun AppTile(
    packageName: String,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    letter: String = letterFor(name),
    color: Color = tileColorFor(packageName)
) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val icon by produceState(AppIcons.cached(packageName, sizePx), packageName, sizePx) {
        if (value == null) value = withContext(Dispatchers.IO) { AppIcons.load(context, packageName, sizePx) }
    }
    val loaded = icon
    if (loaded == null) {
        LetterTile(letter, color, modifier, size)
    } else {
        Image(
            bitmap = loaded,
            contentDescription = null, // decoration: the app name is always next to it
            modifier = modifier
                .size(size)
                .clip(TileShape)
                .background(Color.White) // behind icons with see-through corners
                .border(BorderWidth, Neo.colors.outline, TileShape)
                .clearAndSetSemantics {}
        )
    }
}

/** Loads app icons as small square pictures and keeps the recent ones in memory. */
object AppIcons {
    private val cache = LruCache<String, ImageBitmap>(80)

    fun cached(packageName: String, sizePx: Int): ImageBitmap? = cache.get("$packageName@$sizePx")

    /** null if the app isn't installed (or can't be read). */
    fun load(context: Context, packageName: String, sizePx: Int): ImageBitmap? {
        cached(packageName, sizePx)?.let { return it }
        val drawable = try {
            context.packageManager.getApplicationIcon(packageName).mutate()
        } catch (e: Exception) {
            return null
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (drawable is AdaptiveIconDrawable) {
            // Modern icons are two layers (background + picture) 1.5x bigger than the
            // visible part. Draw both full-square, without the phone's round mask, so
            // every icon fills our rounded tile the same way.
            val extra = sizePx / 4
            listOfNotNull(drawable.background, drawable.foreground).forEach { layer ->
                layer.setBounds(-extra, -extra, sizePx + extra, sizePx + extra)
                layer.draw(canvas)
            }
        } else {
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
        }
        return bitmap.asImageBitmap().also { cache.put("$packageName@$sizePx", it) }
    }
}
