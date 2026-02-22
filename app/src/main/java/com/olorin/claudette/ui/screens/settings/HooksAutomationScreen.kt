package com.olorin.claudette.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Webhook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olorin.claudette.models.ClaudeHook
import com.olorin.claudette.models.ClaudeSettings
import com.olorin.claudette.services.interfaces.ClaudeSettingsServiceInterface
import com.olorin.claudette.ui.theme.ClaudetteError
import com.olorin.claudette.ui.theme.ClaudetteOnSurface
import com.olorin.claudette.ui.theme.ClaudetteOutline
import com.olorin.claudette.ui.theme.ClaudettePrimary
import com.olorin.claudette.ui.theme.ClaudetteSurface
import com.olorin.claudette.ui.theme.ClaudetteSurfaceVariant
import kotlinx.coroutines.launch

private val HOOK_EVENT_TYPES = listOf(
    "PreToolUse",
    "PostToolUse",
    "Notification",
    "Stop",
    "SubagentStop"
)

@Composable
fun HooksAutomationScreen(settingsService: ClaudeSettingsServiceInterface) {
    val settings by settingsService.settings.collectAsState()
    val isLoading by settingsService.isLoading.collectAsState()
    val error by settingsService.error.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (settings == null) {
            settingsService.loadSettings()
        }
    }

    val allHooks: List<Pair<String, ClaudeHook>> = remember(settings) {
        settings?.hooks?.flatMap { (eventType, hooks) ->
            hooks.map { hook -> eventType to hook }
        } ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Webhook,
                    contentDescription = null,
                    tint = ClaudettePrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hooks Automation",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row {
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add hook",
                        tint = ClaudettePrimary
                    )
                }

                Button(
                    onClick = {
                        coroutineScope.launch { settingsService.saveSettings() }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClaudettePrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isLoading && settings != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Save",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        error?.let { errorMessage ->
            Text(
                text = errorMessage,
                color = ClaudetteError,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = ClaudettePrimary,
                    modifier = Modifier.size(48.dp)
                )
            }
        } else if (allHooks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Webhook,
                        contentDescription = null,
                        tint = ClaudetteOutline,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Hooks Configured",
                        color = ClaudetteOnSurface,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap + to add automation hooks",
                        color = ClaudetteOutline,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                items(
                    items = allHooks,
                    key = { (_, hook) -> hook.id }
                ) { (eventType, hook) ->
                    HookItemCard(
                        eventType = eventType,
                        hook = hook,
                        onDelete = {
                            val currentSettings = settings ?: return@HookItemCard
                            val currentHooks = currentSettings.hooks
                                ?.toMutableMap()
                                ?: return@HookItemCard

                            val updatedList = currentHooks[eventType]
                                ?.filter { it.id != hook.id }
                                ?: return@HookItemCard

                            if (updatedList.isEmpty()) {
                                currentHooks.remove(eventType)
                            } else {
                                currentHooks[eventType] = updatedList
                            }

                            settingsService.updateSettings(
                                currentSettings.copy(
                                    hooks = currentHooks.ifEmpty { null }
                                )
                            )
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showAddDialog) {
        AddHookDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { eventType, command, matcher ->
                val currentSettings = settings ?: ClaudeSettings()
                val currentHooks = currentSettings.hooks?.toMutableMap() ?: mutableMapOf()
                val existingList = currentHooks[eventType]?.toMutableList() ?: mutableListOf()

                existingList.add(
                    ClaudeHook(
                        type = eventType,
                        command = command,
                        matcher = matcher.ifBlank { null }
                    )
                )

                currentHooks[eventType] = existingList
                settingsService.updateSettings(currentSettings.copy(hooks = currentHooks))
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun HookItemCard(
    eventType: String,
    hook: ClaudeHook,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = ClaudettePrimary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = eventType,
                            color = ClaudettePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = hook.command,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                hook.matcher?.let { matcher ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Matcher: $matcher",
                        color = ClaudetteOutline,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete hook",
                    tint = ClaudetteError,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddHookDialog(
    onDismiss: () -> Unit,
    onAdd: (eventType: String, command: String, matcher: String) -> Unit
) {
    var selectedEventType by remember { mutableStateOf(HOOK_EVENT_TYPES.first()) }
    var command by remember { mutableStateOf("") }
    var matcher by remember { mutableStateOf("") }
    var eventTypeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ClaudetteSurfaceVariant,
        title = {
            Text(
                text = "Add Hook",
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = eventTypeExpanded,
                    onExpandedChange = { eventTypeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedEventType,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(
                                text = "Event Type",
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = eventTypeExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = hooksTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = eventTypeExpanded,
                        onDismissRequest = { eventTypeExpanded = false },
                        containerColor = ClaudetteSurface
                    ) {
                        HOOK_EVENT_TYPES.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = type,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    selectedEventType = type
                                    eventTypeExpanded = false
                                }
                            )
                        }
                    }
                }

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
                    colors = hooksTextFieldColors()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = matcher,
                    onValueChange = { matcher = it },
                    label = {
                        Text(
                            text = "Matcher (optional)",
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = hooksTextFieldColors()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(selectedEventType, command, matcher) },
                enabled = command.isNotBlank()
            ) {
                Text(
                    text = "Add",
                    color = if (command.isNotBlank()) ClaudettePrimary else ClaudetteOutline,
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
private fun hooksTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ClaudettePrimary,
    unfocusedBorderColor = ClaudetteOutline,
    cursorColor = ClaudettePrimary,
    focusedLabelColor = ClaudettePrimary,
    unfocusedLabelColor = ClaudetteOutline,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)
