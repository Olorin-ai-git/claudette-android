package com.olorin.claudette.ui.screens.profiles

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.olorin.claudette.models.ServerProfile
import com.olorin.claudette.ui.theme.ClaudetteBackground
import com.olorin.claudette.ui.theme.ClaudetteError
import com.olorin.claudette.ui.theme.ClaudetteOnSurface
import com.olorin.claudette.ui.theme.ClaudetteOutline
import com.olorin.claudette.ui.theme.ClaudettePrimary
import com.olorin.claudette.ui.theme.ClaudetteSurface
import com.olorin.claudette.ui.theme.ClaudetteSurfaceVariant
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    onAddProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    onConnect: (profileId: String, path: String) -> Unit,
    onOpenSettings: () -> Unit = {},
    viewModel: ProfileListViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsState()
    val context = LocalContext.current

    // Reload profiles every time this screen becomes visible (e.g. after editor save)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadProfiles()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Only show connect dialog for profiles without a saved project path
    var connectDialogProfile by remember { mutableStateOf<ServerProfile?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = ClaudettePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Claudette",
                            fontFamily = FontFamily.Monospace,
                            color = ClaudettePrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = ClaudetteOutline
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ClaudetteBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProfile,
                containerColor = ClaudettePrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add profile")
            }
        },
        containerColor = ClaudetteBackground
    ) { innerPadding ->
        if (profiles.isEmpty()) {
            EmptyProfileState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                items(
                    items = profiles,
                    key = { it.id }
                ) { profile ->
                    SwipeRevealProfileCard(
                        profile = profile,
                        onTap = {
                            val savedPath = profile.lastProjectPath
                            if (!savedPath.isNullOrBlank()) {
                                // Auto-connect with saved path
                                onConnect(profile.id, savedPath)
                            } else {
                                // First time — ask for project path
                                connectDialogProfile = profile
                            }
                        },
                        onEdit = { onEditProfile(profile.id) },
                        onDelete = { viewModel.deleteProfile(profile.id) },
                        onWakeOnLan = {
                            profile.macAddress?.let { mac ->
                                viewModel.sendWakeOnLan(mac)
                                Toast.makeText(
                                    context,
                                    "Wake-on-LAN sent to ${profile.name}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } ?: run {
                                Toast.makeText(
                                    context,
                                    "No MAC address configured for ${profile.name}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        connectDialogProfile?.let { profile ->
            ConnectDialog(
                profile = profile,
                onDismiss = { connectDialogProfile = null },
                onConnect = { path ->
                    connectDialogProfile = null
                    onConnect(profile.id, path)
                }
            )
        }
    }
}

@Composable
private fun EmptyProfileState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Computer,
            contentDescription = null,
            tint = ClaudetteOutline,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Server Profiles",
            color = ClaudetteOnSurface,
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap + to add your first SSH connection",
            color = ClaudetteOutline,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Profile card with swipe-left-to-reveal Edit and Delete action buttons.
 * Tap to connect, long-press for Wake-on-LAN.
 */
@Composable
private fun SwipeRevealProfileCard(
    profile: ServerProfile,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onWakeOnLan: () -> Unit
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) { ACTION_ROW_WIDTH.toPx() }

    // Raw drag offset accumulator
    var rawOffset by remember { mutableFloatStateOf(0f) }
    // Whether we've snapped open
    var isRevealed by remember { mutableStateOf(false) }

    // Animate toward the target: 0 (closed) or -actionWidthPx (open)
    val targetOffset = if (isRevealed) -actionWidthPx else 0f
    val animatedOffset by animateFloatAsState(
        targetValue = targetOffset,
        animationSpec = tween(durationMillis = 200),
        label = "swipeOffset"
    )

    // Use animatedOffset when not actively dragging, rawOffset while dragging
    var isDragging by remember { mutableStateOf(false) }
    val displayOffset = if (isDragging) rawOffset else animatedOffset

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ACTION_ROW_HEIGHT)
    ) {
        // Action buttons revealed behind the card (right-aligned)
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(ACTION_ROW_HEIGHT)
                .width(ACTION_ROW_WIDTH),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Edit button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(ClaudettePrimary, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .clickable {
                        isRevealed = false
                        rawOffset = 0f
                        onEdit()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Edit",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Delete button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(ClaudetteError, RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                    .clickable {
                        isRevealed = false
                        rawOffset = 0f
                        onDelete()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Delete",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Foreground card — slides left on drag
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(displayOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            // Snap open if dragged past halfway, otherwise snap closed
                            isRevealed = rawOffset < -actionWidthPx * SNAP_THRESHOLD
                            rawOffset = if (isRevealed) -actionWidthPx else 0f
                        },
                        onDragCancel = {
                            isDragging = false
                            rawOffset = if (isRevealed) -actionWidthPx else 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            rawOffset = (rawOffset + dragAmount).coerceIn(-actionWidthPx, 0f)
                        }
                    )
                }
                .clickable {
                    if (isRevealed) {
                        // Tap to close the revealed actions
                        isRevealed = false
                        rawOffset = 0f
                    } else {
                        onTap()
                    }
                },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = ClaudetteSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = ClaudettePrimary,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${profile.username}@${profile.host}:${profile.port}",
                        color = ClaudetteOnSurface,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    profile.lastProjectPath?.let { path ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = path,
                            color = ClaudetteOutline,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    profile.lastConnectedAt?.let { timestamp ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = formatTimestamp(timestamp),
                            color = ClaudetteOutline,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (profile.macAddress != null) {
                    IconButton(onClick = onWakeOnLan) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Wake on LAN",
                            tint = ClaudetteOutline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectDialog(
    profile: ServerProfile,
    onDismiss: () -> Unit,
    onConnect: (String) -> Unit
) {
    var projectPath by remember {
        mutableStateOf(profile.lastProjectPath ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ClaudetteSurfaceVariant,
        title = {
            Text(
                text = "Connect to ${profile.name}",
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column {
                Text(
                    text = "${profile.username}@${profile.host}:${profile.port}",
                    color = ClaudetteOnSurface,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = projectPath,
                    onValueChange = { projectPath = it },
                    label = {
                        Text(
                            text = "Project Path",
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    placeholder = {
                        Text(
                            text = "/home/user/project",
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ClaudettePrimary,
                        unfocusedBorderColor = ClaudetteOutline,
                        cursorColor = ClaudettePrimary,
                        focusedLabelColor = ClaudettePrimary,
                        unfocusedLabelColor = ClaudetteOutline,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConnect(projectPath) },
                enabled = projectPath.isNotBlank()
            ) {
                Text(
                    text = "Connect",
                    color = if (projectPath.isNotBlank()) ClaudettePrimary else ClaudetteOutline,
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

private fun formatTimestamp(epochMillis: Long): String {
    val date = Date(epochMillis)
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(date)
}

private val ACTION_ROW_WIDTH = 160.dp
private val ACTION_ROW_HEIGHT = 88.dp
private const val SNAP_THRESHOLD = 0.4f
