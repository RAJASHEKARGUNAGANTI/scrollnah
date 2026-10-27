package com.unscroll.app.ui.navigation

import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unscroll.app.ui.components.HomeNavIcon
import com.unscroll.app.ui.components.LimitsNavIcon
import com.unscroll.app.ui.components.PauseNavIcon
import com.unscroll.app.ui.components.StatsNavIcon
import com.unscroll.app.ui.theme.Ink
import com.unscroll.app.ui.theme.Lime
import com.unscroll.app.ui.theme.Neo

@Composable
fun UnscrollBottomNav(current: Destination, onSelect: (Destination) -> Unit) {
    Column {
        Box(Modifier.fillMaxWidth().height(2.dp).background(Neo.colors.outline))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Neo.colors.surface)
                // Keep the tabs above the phone's own navigation (3 buttons or gesture bar).
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Destination.entries.forEach { destination ->
                NavItem(destination = destination, selected = destination == current, onClick = { onSelect(destination) })
            }
        }
    }
}

@Composable
private fun NavItem(destination: Destination, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Lime else androidx.compose.ui.graphics.Color.Transparent, shape)
            .border(2.dp, if (selected) Neo.colors.outline else androidx.compose.ui.graphics.Color.Transparent, shape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick) // TalkBack: "Home, tab, selected"
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The selected tab is lime, so its icon and label stay ink in dark mode too.
        CompositionLocalProvider(LocalContentColor provides if (selected) Ink else Neo.colors.content) {
            when (destination) {
                Destination.HOME -> HomeNavIcon()
                Destination.LIMITS -> LimitsNavIcon()
                Destination.STATS -> StatsNavIcon()
                Destination.PAUSE -> PauseNavIcon()
            }
            Text(destination.label, fontWeight = FontWeight.Bold, fontSize = androidx.compose.ui.unit.TextUnit(12f, androidx.compose.ui.unit.TextUnitType.Sp))
        }
    }
}
