package com.unscroll.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unscroll.app.ui.components.CloseIcon
import com.unscroll.app.ui.components.NeoCard
import com.unscroll.app.ui.theme.Neo
import kotlin.random.Random

/*
 * "Do this instead": a short list of things the user would rather do.
 * The Pause screen shows one of them; Settings is where the list is edited.
 */

private const val MAX_IDEAS = 8
private const val MAX_IDEA_LENGTH = 60

/** The Pause screen card: one idea, and "Another idea" to see the next one. Nothing if the list is empty. */
@Composable
internal fun DoThisInsteadCard(ideas: List<String>) {
    if (ideas.isEmpty()) return
    // Start at a random idea, so it isn't always the same one.
    var index by rememberSaveable(ideas) { mutableIntStateOf(Random.nextInt(ideas.size)) }
    // Bottom padding: room for the hard shadow when the card is last in a scrolling column.
    NeoCard(modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)) {
        Text("Try this instead?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            ideas[index % ideas.size],
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 2.dp, bottom = if (ideas.size > 1) 0.dp else 8.dp)
        )
        if (ideas.size > 1) {
            Box(
                Modifier
                    .heightIn(min = 44.dp)
                    .clickable(role = Role.Button) { index = (index + 1) % ideas.size },
                contentAlignment = Alignment.CenterStart
            ) {
                Text("Another idea", fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)
            }
        }
    }
}

/** The Settings list: each idea with a remove button, and a box to add one. */
@Composable
internal fun InsteadIdeasEditor(mascotName: String, ideas: List<String>, onChange: (List<String>) -> Unit) {
    Text(
        "$mascotName shows one of these on the Pause screen. Make them yours.",
        style = MaterialTheme.typography.bodyMedium
    )
    ideas.forEachIndexed { i, idea ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(idea, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) { onChange(ideas.filterIndexed { j, _ -> j != i }) }
                    .semantics { contentDescription = "Remove $idea" },
                contentAlignment = Alignment.Center
            ) { CloseIcon() }
        }
    }
    if (ideas.isEmpty()) {
        Text("No ideas, so the Pause screen won't show this card.", style = MaterialTheme.typography.bodyMedium)
    }
    if (ideas.size < MAX_IDEAS) {
        var draft by rememberSaveable { mutableStateOf("") }
        fun add() {
            val idea = draft.trim()
            if (idea.isNotEmpty() && idea !in ideas) onChange(ideas + idea)
            draft = ""
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it.replace('\n', ' ').take(MAX_IDEA_LENGTH) },
                placeholder = { Text("e.g. Read 2 pages") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { add() }),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Neo.colors.background,
                    unfocusedContainerColor = Neo.colors.background,
                    focusedBorderColor = Neo.colors.outline,
                    unfocusedBorderColor = Neo.colors.outline
                )
            )
            ChoicePill("Add", selected = false, isChoice = false) { add() }
        }
    }
}
