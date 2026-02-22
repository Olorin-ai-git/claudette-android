package com.olorin.claudette.ui.screens.filebrowser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.olorin.claudette.models.RemoteFileEntry
import com.olorin.claudette.models.ServerProfile
import com.olorin.claudette.ui.screens.fileeditor.RemoteFileEditorScreen
import com.olorin.claudette.ui.theme.ClaudetteBackground
import com.olorin.claudette.ui.theme.ClaudetteError
import com.olorin.claudette.ui.theme.ClaudetteOnSurface
import com.olorin.claudette.ui.theme.ClaudetteOutline
import com.olorin.claudette.ui.theme.ClaudettePrimary
import com.olorin.claudette.ui.theme.ClaudetteSurface
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteFileBrowserScreen(
    profile: ServerProfile,
    projectPath: String,
    keychainService: Any?,
    viewModel: RemoteFileBrowserViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var editingFilePath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(profile.id) {
        if (currentPath.isBlank()) {
            viewModel.initialize(
                host = profile.host,
                port = profile.port,
                username = profile.username,
                password = null,
                keyData = null,
                startPath = projectPath
            )
        }
    }

    editingFilePath?.let { filePath ->
        RemoteFileEditorScreen(
            filePath = filePath,
            onDismiss = { editingFilePath = null }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "File Browser",
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    val canNavigateUp = currentPath != "/" && currentPath.isNotBlank()
                    if (canNavigateUp) {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Navigate up",
                                tint = ClaudettePrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ClaudetteBackground
                )
            )
        },
        containerColor = ClaudetteBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Breadcrumb navigation
            if (currentPath.isNotBlank()) {
                BreadcrumbBar(
                    path = currentPath,
                    onSegmentTap = { segmentPath -> viewModel.navigateTo(segmentPath) }
                )
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = ClaudettePrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = ClaudetteError,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = error ?: "Unknown error",
                                color = ClaudetteError,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                entries.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Empty directory",
                            color = ClaudetteOutline,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(4.dp)) }

                        items(
                            items = entries,
                            key = { it.id }
                        ) { entry ->
                            FileEntryCard(
                                entry = entry,
                                onTap = {
                                    if (entry.isDirectory) {
                                        viewModel.navigateTo(entry.path)
                                    } else {
                                        editingFilePath = entry.path
                                    }
                                }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreadcrumbBar(
    path: String,
    onSegmentTap: (String) -> Unit
) {
    val segments = remember(path) {
        buildBreadcrumbSegments(path)
    }
    val scrollState = rememberScrollState()

    LaunchedEffect(segments) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segments.forEachIndexed { index, segment ->
            Text(
                text = segment.name,
                color = if (index == segments.lastIndex) {
                    Color.White
                } else {
                    ClaudettePrimary
                },
                fontSize = 13.sp,
                fontWeight = if (index == segments.lastIndex) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace,
                modifier = if (index < segments.lastIndex) {
                    Modifier.clickable { onSegmentTap(segment.path) }
                } else {
                    Modifier
                }
            )

            if (index < segments.lastIndex) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = ClaudetteOutline,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun FileEntryCard(
    entry: RemoteFileEntry,
    onTap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
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
            Icon(
                imageVector = if (entry.isDirectory) {
                    Icons.Default.Folder
                } else {
                    Icons.AutoMirrored.Filled.InsertDriveFile
                },
                contentDescription = if (entry.isDirectory) "Directory" else "File",
                tint = if (entry.isDirectory) ClaudettePrimary else ClaudetteOnSurface,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    entry.size?.let { size ->
                        if (!entry.isDirectory) {
                            Text(
                                text = formatFileSize(size),
                                color = ClaudetteOutline,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    entry.modifiedAt?.let { timestamp ->
                        if (timestamp > 0) {
                            Text(
                                text = formatTimestamp(timestamp),
                                color = ClaudetteOutline,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            if (entry.isDirectory) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = ClaudetteOutline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private data class BreadcrumbSegment(
    val name: String,
    val path: String
)

private fun buildBreadcrumbSegments(path: String): List<BreadcrumbSegment> {
    val segments = mutableListOf(BreadcrumbSegment(name = "/", path = "/"))
    val parts = path.trimStart('/').split('/').filter { it.isNotEmpty() }
    var accumulated = ""

    for (part in parts) {
        accumulated = "$accumulated/$part"
        segments.add(BreadcrumbSegment(name = part, path = accumulated))
    }

    return segments
}

private const val BYTES_PER_KB = 1024L
private const val BYTES_PER_MB = 1024L * 1024L
private const val BYTES_PER_GB = 1024L * 1024L * 1024L

private fun formatFileSize(bytes: Long): String = when {
    bytes >= BYTES_PER_GB -> "%.1f GB".format(bytes.toDouble() / BYTES_PER_GB)
    bytes >= BYTES_PER_MB -> "%.1f MB".format(bytes.toDouble() / BYTES_PER_MB)
    bytes >= BYTES_PER_KB -> "%.1f KB".format(bytes.toDouble() / BYTES_PER_KB)
    else -> "$bytes B"
}

private fun formatTimestamp(epochMillis: Long): String {
    val date = Date(epochMillis)
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(date)
}
