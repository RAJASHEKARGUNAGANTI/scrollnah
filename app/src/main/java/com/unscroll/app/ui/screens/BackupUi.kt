package com.unscroll.app.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.unscroll.app.appContainer
import com.unscroll.app.data.Backup
import com.unscroll.app.ui.theme.Neo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * "Restore a backup": pick the file (Android's own file screen, no permission needed),
 * check it, ask "Restore this backup?", restore, then restart ScrollNah.
 * [content] draws the button or row and gets the function that starts it all.
 * Used in Settings and on the first setup screen (a new phone).
 */
@Composable
internal fun RestoreBackupAction(content: @Composable (start: () -> Unit) -> Unit) {
    val context = LocalContext.current
    val container = appContainer()
    val scope = rememberCoroutineScope()
    // A backup file that was picked and read, waiting for "Restore?" to be confirmed.
    var pending by remember { mutableStateOf<Pair<JSONObject, Backup.Summary>?>(null) }

    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            try {
                pending = Backup.read(context, container, uri)
            } catch (e: Exception) {
                Toast.makeText(context, (e as? Backup.BackupException)?.message ?: "Couldn't read that file.", Toast.LENGTH_LONG).show()
            }
        }
    }
    content { openBackup.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }

    pending?.let { (json, summary) ->
        val date = Instant.ofEpochMilli(summary.exportedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        AlertDialog(
            onDismissRequest = { pending = null },
            containerColor = Neo.colors.surface,
            title = { Text("Restore this backup?", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(
                    "Saved on ${date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))} (ScrollNah ${summary.appVersion}): " +
                        "${summary.apps} ${if (summary.apps == 1) "app" else "apps"}, ${summary.days} saved days, " +
                        "${summary.pots} ${if (summary.pots == 1) "pot" else "pots"} in the garden.\n\n" +
                        "This replaces everything in ScrollNah on this phone. ScrollNah restarts when it's done."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    container.appScope.launch {
                        val ok = try {
                            Backup.restore(container, json)
                            true
                        } catch (e: Exception) {
                            false
                        }
                        withContext(Dispatchers.Main) {
                            if (ok) restartApp(context)
                            else Toast.makeText(context, "Couldn't restore that backup. Nothing was changed.", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Restore", fontWeight = FontWeight.Bold, color = Neo.colors.content) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel", color = Neo.colors.content) } }
        )
    }
}

/** Starts ScrollNah fresh, so every screen, the blocker and the widgets pick up the restored data. */
private fun restartApp(context: Context) {
    Toast.makeText(context, "Backup restored", Toast.LENGTH_SHORT).show()
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
    android.os.Process.killProcess(android.os.Process.myPid())
}
