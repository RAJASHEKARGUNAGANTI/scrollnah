package com.unscroll.app.ui.components

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.unscroll.app.ui.theme.BorderWidth
import com.unscroll.app.ui.theme.Neo

/**
 * "Search your apps" box with a magnifying glass and a clear (X) button.
 * The keyboard's search key just closes the keyboard (the list filters as you type).
 */
@Composable
fun AppSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text("Search your apps") },
        leadingIcon = { SearchIcon() },
        trailingIcon = if (query.isNotEmpty()) {
            {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) { onQueryChange("") }
                        .semantics { contentDescription = "Clear search" },
                    contentAlignment = Alignment.Center
                ) { CloseIcon() }
            }
        } else null,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Neo.colors.surface,
            unfocusedContainerColor = Neo.colors.surface,
            focusedBorderColor = Neo.colors.outline,
            unfocusedBorderColor = Neo.colors.outline,
            focusedLeadingIconColor = Neo.colors.content,
            unfocusedLeadingIconColor = Neo.colors.content
        )
    )
}
