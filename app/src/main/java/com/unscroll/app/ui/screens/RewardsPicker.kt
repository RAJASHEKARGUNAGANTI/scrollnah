package com.unscroll.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unscroll.app.data.MascotReward
import com.unscroll.app.data.MascotTheme
import com.unscroll.app.ui.theme.BorderWidth
import com.unscroll.app.ui.theme.Neo
import com.unscroll.app.ui.theme.PillShape

/**
 * Settings > Mascot: the hats (Blobby) or pot colours (Bloomy) earned with streaks.
 * Locked ones show the streak they need. [best] = the best streak ever.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun RewardsPicker(theme: MascotTheme, best: Int, selected: MascotReward?, onPick: (MascotReward?) -> Unit) {
    val hats = theme == MascotTheme.BLOBBY
    Text(if (hats) "Hats" else "Pot colours", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
    Text(
        "${theme.mascotName} earns one at 3, 7, 14 and 30 days under your limits in a row. " +
            "Best streak so far: $best ${if (best == 1) "day" else "days"}.",
        style = MaterialTheme.typography.bodyMedium
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoicePill(if (hats) "No hat" else "Classic", selected = selected == null) { onPick(null) }
        MascotReward.forTheme(theme).forEach { reward ->
            if (reward.unlocked(best)) {
                ChoicePill(reward.title, selected = selected == reward) { onPick(reward) }
            } else {
                LockedPill(reward)
            }
        }
    }
}

/** A reward not earned yet: greyed out, with the streak it needs. */
@Composable
private fun LockedPill(reward: MascotReward) {
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .alpha(0.5f)
            .border(BorderWidth, Neo.colors.outline, PillShape)
            .clearAndSetSemantics { contentDescription = "${reward.title}, locked: needs a ${reward.unlockDays}-day streak" }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("${reward.title} · ${reward.unlockDays} days", color = Neo.colors.content)
    }
}
