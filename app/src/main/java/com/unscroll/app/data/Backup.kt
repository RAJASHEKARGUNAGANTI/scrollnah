package com.unscroll.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.unscroll.app.AppContainer
import com.unscroll.app.service.FocusRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Backup and restore: everything ScrollNah keeps (limits, history, streaks, garden, settings,
 * bedtime, work hours) in one JSON file the user saves wherever they like. Nothing is uploaded.
 *
 * Database tables are copied generically (every table, every column), so tables and columns
 * added later are included without changing this file. A backup from an older version restores
 * into a newer app (missing columns get their defaults); a backup from a newer app is refused.
 */
object Backup {
    private const val APP = "ScrollNah"
    private const val FORMAT = 1

    /** Room's own bookkeeping tables, never copied. */
    private val internalTables = setOf("android_metadata", "room_master_table", "sqlite_sequence", "room_table_modification_log")

    /** What a backup file contains, for the "Restore?" question. */
    data class Summary(val exportedAt: Long, val appVersion: String, val apps: Int, val days: Int, val pots: Int)

    class BackupException(message: String) : Exception(message)

    /** Writes the backup to [uri] (a file the user picked). */
    suspend fun write(context: Context, container: AppContainer, uri: Uri) = withContext(Dispatchers.IO) {
        val db = container.database.openHelper.readableDatabase
        val tables = JSONObject()
        for (table in userTables(db)) {
            val rows = JSONArray()
            db.query("SELECT * FROM `$table`").use { c ->
                while (c.moveToNext()) {
                    val row = JSONObject()
                    for (i in 0 until c.columnCount) row.put(c.getColumnName(i), valueAt(c, i))
                    rows.put(row)
                }
            }
            tables.put(table, rows)
        }
        val bedtime = FocusRules.bedtime()
        val work = FocusRules.workHours()
        val json = JSONObject()
            .put("app", APP)
            .put("format", FORMAT)
            .put("exportedAt", System.currentTimeMillis())
            .put("appVersion", appVersion(context))
            .put("dbVersion", db.version)
            .put("settings", container.settings.exportJson())
            .put("bedtime", JSONObject().put("enabled", bedtime.enabled).put("start", bedtime.startMinutes).put("end", bedtime.endMinutes))
            .put(
                "workHours",
                JSONObject().put("enabled", work.enabled).put("start", work.startMinutes).put("end", work.endMinutes)
                    .put("days", JSONArray(work.days.sorted().map { it.value }))
            )
            .put("tables", tables)
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: throw BackupException("Couldn't open that file.")
        out.use { it.write(json.toString().toByteArray()) }
    }

    /** Reads and checks a backup file without changing anything. */
    suspend fun read(context: Context, container: AppContainer, uri: Uri): Pair<JSONObject, Summary> = withContext(Dispatchers.IO) {
        val text = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (e: Exception) {
            null
        } ?: throw BackupException("Couldn't read that file.")
        val json = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw BackupException("That isn't a ScrollNah backup.")
        }
        if (json.optString("app") != APP || !json.has("tables")) throw BackupException("That isn't a ScrollNah backup.")
        if (json.optInt("format") > FORMAT || json.optInt("dbVersion") > container.database.openHelper.readableDatabase.version) {
            throw BackupException("This backup is from a newer ScrollNah. Update the app first, then restore.")
        }
        val tables = json.getJSONObject("tables")
        fun count(table: String) = tables.optJSONArray(table)?.length() ?: 0
        json to Summary(
            exportedAt = json.optLong("exportedAt"),
            appVersion = json.optString("appVersion", "?"),
            apps = count("tracked_apps"),
            days = count("daily_summary"),
            pots = count("garden_pots")
        )
    }

    /** Replaces everything in ScrollNah with the backup in [json] (from [read]). */
    suspend fun restore(container: AppContainer, json: JSONObject) = withContext(Dispatchers.IO) {
        val roomDb = container.database
        val db = roomDb.openHelper.writableDatabase
        val tables = json.getJSONObject("tables")
        roomDb.runInTransaction {
            for (table in userTables(db)) {
                val columns = columnsOf(db, table)
                db.execSQL("DELETE FROM `$table`")
                val rows = tables.optJSONArray(table) ?: continue
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val values = ContentValues()
                    // Only columns this version knows; ones the backup doesn't have keep their defaults.
                    row.keys().forEach { col -> if (col in columns) putValue(values, col, row.get(col)) }
                    db.insert(table, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE, values)
                }
            }
        }
        container.settings.importJson(json.optJSONObject("settings") ?: JSONObject())
        json.optJSONObject("bedtime")?.let {
            FocusRules.setBedtime(FocusRules.Bedtime(it.optBoolean("enabled"), it.optInt("start", 22 * 60), it.optInt("end", 7 * 60)))
        }
        json.optJSONObject("workHours")?.let {
            val days = it.optJSONArray("days")?.let { a -> (0 until a.length()).map { i -> java.time.DayOfWeek.of(a.getInt(i)) }.toSet() }
            val default = FocusRules.WorkHours.DEFAULT
            FocusRules.setWorkHours(
                FocusRules.WorkHours(it.optBoolean("enabled"), it.optInt("start", default.startMinutes), it.optInt("end", default.endMinutes), days ?: default.days)
            )
        }
    }

    /** "ScrollNah-backup-2026-11-01.json" */
    fun fileName(): String = "ScrollNah-backup-${java.time.LocalDate.now()}.json"

    private fun userTables(db: androidx.sqlite.db.SupportSQLiteDatabase): List<String> {
        val names = mutableListOf<String>()
        db.query("SELECT name FROM sqlite_master WHERE type = 'table'").use { c ->
            while (c.moveToNext()) names += c.getString(0)
        }
        return names.filter { it !in internalTables && !it.startsWith("sqlite_") }
    }

    private fun columnsOf(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String): Set<String> {
        val cols = mutableSetOf<String>()
        db.query("PRAGMA table_info(`$table`)").use { c ->
            val nameIndex = c.getColumnIndexOrThrow("name")
            while (c.moveToNext()) cols += c.getString(nameIndex)
        }
        return cols
    }

    private fun valueAt(c: Cursor, i: Int): Any = when (c.getType(i)) {
        Cursor.FIELD_TYPE_INTEGER -> c.getLong(i)
        Cursor.FIELD_TYPE_FLOAT -> c.getDouble(i)
        Cursor.FIELD_TYPE_STRING -> c.getString(i)
        else -> JSONObject.NULL // NULL (no table stores blobs)
    }

    private fun putValue(values: ContentValues, col: String, v: Any?) {
        when (v) {
            null, JSONObject.NULL -> values.putNull(col)
            is Int -> values.put(col, v.toLong())
            is Long -> values.put(col, v)
            is Double -> values.put(col, v)
            is Boolean -> values.put(col, if (v) 1L else 0L)
            else -> values.put(col, v.toString())
        }
    }

    private fun appVersion(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
}
