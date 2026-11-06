package com.unscroll.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unscroll.app.ui.theme.Neo

/**
 * What the optional Reels/Shorts & website tracker (an Accessibility service) reads and does,
 * shown before the user is sent to Android's Accessibility settings. Google Play asks for this
 * "prominent disclosure": in the app, before turning it on, saying what is read and how it's
 * used, with a button to agree and a way to say no. The Limits card and the Settings dialog
 * show the same words, so keep them here.
 */
object TrackerDisclosure {
    const val TITLE = "Limit Reels, Shorts & websites"

    /** Always shown above the "Agree" button. */
    const val WHAT =
        "With your OK, ScrollNah uses Android's Accessibility service to see which screen is open in " +
            "Instagram and YouTube, when their videos scroll, and your browser's address bar. It uses this " +
            "only to time Reels, Shorts and limited websites, count the videos, and press Back once to leave " +
            "Reels or Shorts when you reach your limit. Only minutes and the count are saved, on this phone. " +
            "Nothing is sent anywhere, and it never reads messages or passwords."

    /** The extra notes (on the Limits card behind the chevron). */
    const val NOTES =
        "Some UPI and bank apps (like BHIM) won't open while any app uses Accessibility. Turn the tracker " +
            "off while you pay; your other limits keep working.\n\n" +
            "In Settings: Downloaded apps > ScrollNah > turn on. You can turn it off there any time."

    const val AGREE = "Agree & open Settings"
    const val DECLINE = "Not now"
}

/** The disclosure as a dialog (Settings > Permissions). [onAgree] opens Accessibility settings. */
@Composable
fun TrackerDisclosureDialog(onAgree: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Neo.colors.surface,
        title = { Text(TrackerDisclosure.TITLE, style = MaterialTheme.typography.headlineSmall) },
        text = {
            // Scrolls with very big text, so the whole disclosure can always be read.
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(TrackerDisclosure.WHAT, style = MaterialTheme.typography.bodyMedium)
                Text(TrackerDisclosure.NOTES, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onAgree) { Text(TrackerDisclosure.AGREE, fontWeight = FontWeight.Bold, color = Neo.colors.content) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(TrackerDisclosure.DECLINE, color = Neo.colors.content) } }
    )
}
