package com.unscroll.app.data

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.unscroll.app.AppContainer
import com.unscroll.app.data.db.AppGroup
import com.unscroll.app.data.db.FeatureLimit
import com.unscroll.app.data.db.TrackedApp
import com.unscroll.app.service.FocusRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * "Copy limits to computer" (Settings > Data): a small file with only the limits and a few settings,
 * for ScrollNah's browser extension (Chrome, Edge, Brave, Opera, Firefox), which turns each app into
 * its website (YouTube app -> youtube.com). No history, no stats. Shared through Android's share menu
 * (email it to yourself, Quick Share, Drive...); ScrollNah itself sends nothing.
 *
 * Its "app" field is "ScrollNah limits", not "ScrollNah", so it can never be restored as a backup
 * (older app versions refuse it too). The extension reads it in extension/src/lib/phone.ts.
 */
object LimitsExport {
    const val APP = "ScrollNah limits"
    const val FORMAT = 1
    const val FILE_NAME = "scrollnah-limits.json"

    data class Input(
        val apps: List<TrackedApp>,
        val groups: List<AppGroup>,
        val features: List<FeatureLimit>,
        val name: String,
        val mascot: MascotTheme,
        val flowers: Int,
        val strictness: Strictness,
        val bedtime: FocusRules.Bedtime,
        val work: FocusRules.WorkHours,
        val appVersion: String,
        val exportedAt: Long
    )

    /** The file's text (pure, tested in LimitsExportTest). */
    fun json(input: Input): String = obj(
        "app" to str(APP),
        "format" to FORMAT.toString(),
        "exportedAt" to input.exportedAt.toString(),
        "appVersion" to str(input.appVersion),
        "name" to str(input.name),
        "mascot" to str(input.mascot.name),
        "flowers" to input.flowers.toString(),
        "strictness" to str(input.strictness.name),
        "bedtime" to obj(
            "enabled" to input.bedtime.enabled.toString(),
            "start" to input.bedtime.startMinutes.toString(),
            "end" to input.bedtime.endMinutes.toString()
        ),
        "workHours" to obj(
            "enabled" to input.work.enabled.toString(),
            "start" to input.work.startMinutes.toString(),
            "end" to input.work.endMinutes.toString(),
            "days" to arr(input.work.days.map { it.value }.sorted().map { it.toString() })
        ),
        "apps" to arr(input.apps.map { a ->
            obj(
                "packageName" to str(a.packageName),
                "appName" to str(a.appName),
                "limitMinutes" to a.limitMinutes.toString(),
                "weekendLimitMinutes" to num(a.weekendLimitMinutes),
                "maxOpens" to num(a.maxOpens),
                "enabled" to a.enabled.toString()
            )
        }),
        "groups" to arr(input.groups.map { g ->
            obj(
                "name" to str(g.name),
                "limitMinutes" to g.limitMinutes.toString(),
                "enabled" to g.enabled.toString(),
                "packages" to arr(g.members().sorted().map { str(it) })
            )
        }),
        // Only minute limits: the daily reels count can't be counted on websites.
        "features" to arr(input.features.filter { it.featureId != ReelLimit.ID }.map { f ->
            obj("id" to str(f.featureId), "limitMinutes" to f.limitMinutes.toString(), "enabled" to f.enabled.toString())
        })
    )

    /** Writes the file to the app's cache (the only folder ScrollNah shares from) and opens the share menu. */
    suspend fun share(context: Context, container: AppContainer) {
        val input = Input(
            apps = container.database.trackedAppDao().getAll(),
            groups = container.database.appGroupDao().getAll(),
            features = container.database.featureDao().observeLimits().first(),
            name = container.settings.userName.first(),
            mascot = container.settings.mascotTheme.first(),
            flowers = container.settings.potProgress.first().flowers,
            strictness = container.settings.strictness.first(),
            bedtime = FocusRules.bedtime(),
            work = FocusRules.workHours(),
            appVersion = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?",
            exportedAt = System.currentTimeMillis()
        )
        val file = withContext(Dispatchers.IO) {
            File(context.cacheDir, "shared").apply { mkdirs() }.resolve(FILE_NAME).also { it.writeText(json(input)) }
        }
        withContext(Dispatchers.Main) {
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                val send = Intent(Intent.ACTION_SEND)
                    .setType("application/json")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .putExtra(Intent.EXTRA_SUBJECT, "My ScrollNah limits")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                send.clipData = ClipData.newRawUri(null, uri)
                context.startActivity(Intent.createChooser(send, "Send your limits to your computer"))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "No app found to share with", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---- A tiny JSON writer (keeps this testable on the JVM without Android's org.json) ----

    private fun str(s: String): String = buildString {
        append('"')
        for (c in s) when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
        }
        append('"')
    }

    private fun num(n: Int?): String = n?.toString() ?: "null"
    private fun obj(vararg pairs: Pair<String, String>): String = pairs.joinToString(",", "{", "}") { (k, v) -> "${str(k)}:$v" }
    private fun arr(items: List<String>): String = items.joinToString(",", "[", "]")
}
