package com.olorin.claudette.ui.screens.session

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.olorin.claudette.LocalAppConfiguration
import com.olorin.claudette.models.ConnectionState
import com.olorin.claudette.models.HostKeyVerificationResult
import com.olorin.claudette.ui.components.ConnectionStatusHud
import com.olorin.claudette.ui.components.ExtendedKeyboardAccessory
import com.olorin.claudette.ui.screens.agents.AgentVisualizerScreen
import com.olorin.claudette.ui.screens.filebrowser.RemoteFileBrowserScreen
import com.olorin.claudette.ui.screens.snippets.SnippetDrawerScreen
import com.olorin.claudette.ui.theme.ClaudetteBackground
import com.olorin.claudette.ui.theme.ClaudetteError
import com.olorin.claudette.ui.theme.ClaudettePrimary
import com.olorin.claudette.ui.theme.ClaudetteSurface
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    profileId: String,
    projectPath: String,
    onDisconnect: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val config = LocalAppConfiguration.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val tabs by viewModel.tabs.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val connectionState by viewModel.activeConnectionState.collectAsState()
    val hostKeyAlert by viewModel.hostKeyAlert.collectAsState()
    val networkStatus by viewModel.networkStatus.collectAsState()
    val detectedAuthUrl by viewModel.detectedAuthUrl.collectAsState()

    var showSnippetSheet by remember { mutableStateOf(false) }
    var showFileBrowserSheet by remember { mutableStateOf(false) }
    var showAgentSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Image picker for Paste Img
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes() ?: return@launch
                    inputStream.close()
                    val remotePath = "/tmp/claudette_paste_${System.currentTimeMillis()}.png"
                    viewModel.getActiveConnectionManager()?.uploadData(bytes, remotePath)
                    val controller = viewModel.getActiveTerminalController()
                    controller?.sendInput("$remotePath\n")
                    Toast.makeText(context, "Image uploaded", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(profileId, projectPath) {
        viewModel.initialize(profileId, projectPath)
    }

    // Host key verification dialog
    hostKeyAlert?.let { alert ->
        HostKeyAlertDialog(
            alert = alert,
            onAccept = { viewModel.acceptHostKey() },
            onReject = { viewModel.rejectHostKey() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = viewModel.profile.collectAsState().value?.name ?: "",
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        ConnectionStatusHud(networkStatus = networkStatus)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.disconnect()
                        onDisconnect()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Connection state indicator
                    ConnectionStateIcon(connectionState)

                    IconButton(onClick = { showSnippetSheet = true }) {
                        Icon(Icons.Default.Code, contentDescription = "Snippets", tint = Color.White)
                    }
                    IconButton(onClick = { showFileBrowserSheet = true }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Files", tint = Color.White)
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Agent Visualizer") },
                                leadingIcon = { Icon(Icons.Default.Memory, null) },
                                onClick = {
                                    showMenu = false
                                    showAgentSheet = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy Session") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                                onClick = {
                                    showMenu = false
                                    val content = viewModel.copySessionToClipboard()
                                    if (content != null) {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("Session", content))
                                        Toast.makeText(context, "Session copied", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reconnect") },
                                leadingIcon = { Icon(Icons.Default.Refresh, null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.reconnect()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ClaudetteSurface
                )
            )
        },
        containerColor = ClaudetteBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Auth URL banner
            detectedAuthUrl?.let { url ->
                AuthUrlBanner(
                    url = url,
                    onDismiss = { viewModel.clearDetectedAuthUrl() },
                    context = context
                )
            }

            // Tab bar (only show if >1 tab)
            if (tabs.size > 1) {
                TabBar(
                    tabs = tabs,
                    activeTabId = activeTabId,
                    onSelectTab = { viewModel.selectTab(it) },
                    onCloseTab = { viewModel.closeTab(it) },
                    onAddTab = { viewModel.addTab() }
                )
            }

            // Terminal content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(android.graphics.Color.parseColor(config.terminalBackgroundColor)))
            ) {
                val activeController = viewModel.getActiveTerminalController()
                if (activeController != null) {
                    TerminalView(
                        controller = activeController,
                        config = config,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Show connection state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (connectionState) {
                            is ConnectionState.Connecting -> Text("Connecting...", color = Color.White)
                            is ConnectionState.Reconnecting -> {
                                val r = connectionState as ConnectionState.Reconnecting
                                Text("Reconnecting ${r.attempt}/${r.maxAttempts}...", color = Color.White)
                            }
                            is ConnectionState.Failed -> {
                                val f = connectionState as ConnectionState.Failed
                                Text("Failed: ${f.errorDescription}", color = ClaudetteError)
                            }
                            else -> Text("Disconnected", color = Color.Gray)
                        }
                    }
                }
            }

            // Extended keyboard accessory
            ExtendedKeyboardAccessory(
                buttons = config.keyboardAccessoryButtons,
                backgroundColor = Color(android.graphics.Color.parseColor(config.keyboardAccessoryBackgroundColor)),
                buttonColor = Color(android.graphics.Color.parseColor(config.keyboardAccessoryButtonColor)),
                buttonTextColor = Color(android.graphics.Color.parseColor(config.keyboardAccessoryButtonTextColor)),
                onButtonClick = { button ->
                    if (button.action == "paste_image") {
                        imagePicker.launch("image/*")
                    } else if (button.byteSequence.isNotEmpty()) {
                        val bytes = button.byteSequence.map { it.toByte() }.toByteArray()
                        viewModel.getActiveTerminalController()?.sendInput(bytes)
                    }
                },
                onDismissKeyboard = { focusManager.clearFocus() },
                modifier = Modifier.imePadding()
            )
        }
    }

    // Bottom sheets
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showSnippetSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSnippetSheet = false },
            sheetState = sheetState,
            containerColor = ClaudetteSurface
        ) {
            SnippetDrawerScreen(
                onSnippetSelected = { command ->
                    showSnippetSheet = false
                    viewModel.sendSnippet(command)
                }
            )
        }
    }

    if (showFileBrowserSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFileBrowserSheet = false },
            sheetState = sheetState,
            containerColor = ClaudetteSurface
        ) {
            val profile = viewModel.profile.collectAsState().value
            if (profile != null) {
                RemoteFileBrowserScreen(
                    profile = profile,
                    projectPath = projectPath,
                    keychainService = viewModel.let {
                        // Access via DI - this is injected into the ViewModel
                        null // Will be wired properly in the actual screen
                    }
                )
            }
        }
    }

    if (showAgentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAgentSheet = false },
            sheetState = sheetState,
            containerColor = ClaudetteSurface
        ) {
            AgentVisualizerScreen(agentParser = viewModel.agentParser)
        }
    }
}

@Composable
private fun AuthUrlBanner(url: String, onDismiss: () -> Unit, context: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ClaudettePrimary.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Auth URL copied!",
            color = ClaudettePrimary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Open",
            color = ClaudettePrimary,
            fontSize = 13.sp,
            modifier = Modifier.clickable {
                try {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(url)
                    )
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Close, "Dismiss", tint = ClaudettePrimary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun TabBar(
    tabs: List<com.olorin.claudette.models.TerminalTab>,
    activeTabId: String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onAddTab: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ClaudetteSurface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOfFirst { it.id == activeTabId }.coerceAtLeast(0),
            modifier = Modifier.weight(1f),
            containerColor = ClaudetteSurface,
            edgePadding = 0.dp,
            divider = {}
        ) {
            tabs.forEach { tab ->
                Tab(
                    selected = tab.id == activeTabId,
                    onClick = { onSelectTab(tab.id) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = tab.label,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                            if (tabs.size > 1) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close tab",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onCloseTab(tab.id) },
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                )
            }
        }
        IconButton(onClick = onAddTab, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Add, "Add tab", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ConnectionStateIcon(state: ConnectionState) {
    val (color, label) = when (state) {
        is ConnectionState.Connected -> Color(0xFF22C55E) to "Connected"
        is ConnectionState.Connecting -> Color(0xFFF59E0B) to "Connecting"
        is ConnectionState.Reconnecting -> Color(0xFFF59E0B) to "Reconnecting"
        is ConnectionState.Failed -> ClaudetteError to "Failed"
        is ConnectionState.Disconnected -> Color.Gray to "Disconnected"
    }

    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape)
    )
}

@Composable
fun HostKeyAlertDialog(
    alert: HostKeyAlertState,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val title = when (alert.result) {
        is HostKeyVerificationResult.NewHost -> "New Host"
        is HostKeyVerificationResult.KeyChanged -> "WARNING: Host Key Changed!"
        is HostKeyVerificationResult.Trusted -> return
    }

    val message = when (alert.result) {
        is HostKeyVerificationResult.NewHost ->
            "Connecting to ${alert.hostIdentifier} for the first time.\n\nFingerprint:\n${alert.fingerprint}\n\nDo you trust this host?"
        is HostKeyVerificationResult.KeyChanged -> {
            val kc = alert.result as HostKeyVerificationResult.KeyChanged
            "The host key for ${alert.hostIdentifier} has CHANGED!\n\n" +
                "Previous: ${kc.previousFingerprint}\n" +
                "New: ${kc.newFingerprint}\n\n" +
                "This could indicate a security threat. Accept new key?"
        }
        is HostKeyVerificationResult.Trusted -> return
    }

    AlertDialog(
        onDismissRequest = { onReject() },
        title = { Text(title) },
        text = {
            Text(
                text = message,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text("Accept", color = ClaudettePrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onReject) {
                Text("Reject", color = ClaudetteError)
            }
        },
        containerColor = ClaudetteSurface
    )
}
