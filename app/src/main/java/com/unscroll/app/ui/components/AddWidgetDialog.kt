package com.unscroll.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unscroll.app.data.MoodLevel
import com.unscroll.app.ui.mascot.LocalMascot
import com.unscroll.app.ui.mascot.Mascot
import com.unscroll.app.ui.theme.Neo
import com.unscroll.app.widget.MascotWidgets

/**
 * "Add a widget": pick one of the three widgets, then the phone's home screen asks where to
 * put it (its own confirmation, with a preview). Home screens that can't do this get the
 * manual steps instead. [onAdded] runs once the home screen was asked.
 */
@Composable
fun AddWidgetDialog(onDismiss: () -> Unit, onAdded: () -> Unit = {}) {
    val context = LocalContext.current
    val canPin = remember { MascotWidgets.canPin(context) }
    val name = LocalMascot.current.theme.mascotName

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Neo.colors.surface,
        title = { Text("Add a widget", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (canPin) {
                    Text("$name on your home screen, in today's mood. Pick one, then your phone asks where to put it.")
                    MascotWidgets.available.forEach { kind ->
                        WidgetOption(kind) {
                            if (MascotWidgets.pin(context, kind)) {
                                onAdded()
                                onDismiss()
                            } else {
                                Toast.makeText(context, MANUAL_STEPS, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    Text(MANUAL_STEPS)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (canPin) "Cancel" else "OK", fontWeight = FontWeight.Bold, color = Neo.colors.content)
            }
        }
    )
}

private const val MANUAL_STEPS =
    "Long-press an empty spot on your home screen, tap Widgets, then find ScrollNah."

@Composable
private fun WidgetOption(kind: MascotWidgets.Kind, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .border(2.dp, Neo.colors.outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Mascot(mood = MoodLevel.HAPPY, size = 40.dp, animated = false)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(kind.title, fontWeight = FontWeight.Bold)
            Text(kind.size, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
