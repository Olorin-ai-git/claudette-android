package com.olorin.claudette.ui.screens.snippets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.olorin.claudette.models.PromptSnippet
import com.olorin.claudette.models.SnippetCategory
import com.olorin.claudette.ui.theme.ClaudetteBackground
import com.olorin.claudette.ui.theme.ClaudetteOnSurface
import com.olorin.claudette.ui.theme.ClaudetteOutline
import com.olorin.claudette.ui.theme.ClaudettePrimary
import com.olorin.claudette.ui.theme.ClaudetteSurface
import com.olorin.claudette.ui.theme.ClaudetteSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetDrawerScreen(
    onSnippetSelected: (String) -> Unit,
    viewModel: SnippetDrawerViewModel = hiltViewModel()
) {
    val filteredSnippets by viewModel.filteredSnippets.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val groupedSnippets = remember(filteredSnippets) {
        filteredSnippets.groupBy { it.category }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Drag handle indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .padding(bottom = 0.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = {
                Text(
                    text = "Search snippets...",
                    fontFamily = FontFamily.Monospace,
                    color = ClaudetteOutline
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = ClaudetteOutline
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = searchQuery.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = ClaudetteOutline
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ClaudettePrimary,
                unfocusedBorderColor = ClaudetteOutline,
                cursorColor = ClaudettePrimary,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = ClaudetteSurface,
                unfocusedContainerColor = ClaudetteSurface
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Snippet list grouped by category
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SnippetCategory.entries.forEach { category ->
                val snippetsInCategory = groupedSnippets[category]
                if (!snippetsInCategory.isNullOrEmpty()) {
                    item(key = "header_${category.name}") {
                        CategoryHeader(category = category)
                    }

                    items(
                        items = snippetsInCategory,
                        key = { it.id }
                    ) { snippet ->
                        SnippetCard(
                            snippet = snippet,
                            onTap = { onSnippetSelected(snippet.command) },
                            onDelete = if (!snippet.isBuiltIn) {
                                { viewModel.deleteSnippet(snippet.id) }
                            } else {
                                null
                            }
                        )
                    }

                    item(key = "spacer_${category.name}") {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(60.dp)) }
        }

        // Add Custom button
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ClaudettePrimary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add Custom Snippet",
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }
    }

    if (showAddDialog) {
        AddSnippetDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { label, command, category ->
                viewModel.addSnippet(label, command, category)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun CategoryHeader(category: SnippetCategory) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = null,
            tint = ClaudettePrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = category.displayName,
            color = ClaudettePrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SnippetCard(
    snippet: PromptSnippet,
    onTap: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ClaudetteSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = snippet.label,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = snippet.command,
                    color = ClaudetteOnSurface,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete snippet",
                        tint = ClaudetteOutline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSnippetDialog(
    onDismiss: () -> Unit,
    onAdd: (label: String, command: String, category: SnippetCategory) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SnippetCategory.CUSTOM) }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ClaudetteSurfaceVariant,
        title = {
            Text(
                text = "Add Custom Snippet",
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = {
                        Text(
                            text = "Label",
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = snippetTextFieldColors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = command,
                    onValueChange = { command = it },
                    label = {
                        Text(
                            text = "Command",
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = snippetTextFieldColors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(
                                text = "Category",
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = snippetTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                        containerColor = ClaudetteSurface
                    ) {
                        SnippetCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = categoryIcon(category),
                                            contentDescription = null,
                                            tint = ClaudettePrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = category.displayName,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 14.sp
                                        )
                                    }
                                },
                                onClick = {
                                    selectedCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(label, command, selectedCategory) },
                enabled = label.isNotBlank() && command.isNotBlank()
            ) {
                Text(
                    text = "Add",
                    color = if (label.isNotBlank() && command.isNotBlank()) {
                        ClaudettePrimary
                    } else {
                        ClaudetteOutline
                    },
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = ClaudetteOnSurface,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    )
}

@Composable
private fun snippetTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ClaudettePrimary,
    unfocusedBorderColor = ClaudetteOutline,
    cursorColor = ClaudettePrimary,
    focusedLabelColor = ClaudettePrimary,
    unfocusedLabelColor = ClaudetteOutline,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)

private fun categoryIcon(category: SnippetCategory): ImageVector = when (category) {
    SnippetCategory.CLAUDE_COMMANDS -> Icons.Default.Terminal
    SnippetCategory.REFACTORING -> Icons.Default.Sync
    SnippetCategory.DEBUGGING -> Icons.Default.BugReport
    SnippetCategory.GIT -> Icons.Default.Terminal
    SnippetCategory.CUSTOM -> Icons.Default.Star
}
