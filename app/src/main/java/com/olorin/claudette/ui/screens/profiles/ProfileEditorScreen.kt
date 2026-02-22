package com.olorin.claudette.ui.screens.profiles

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.olorin.claudette.models.BonjourHost
import com.olorin.claudette.ui.theme.ClaudetteBackground
import com.olorin.claudette.ui.theme.ClaudetteOnSurface
import com.olorin.claudette.ui.theme.ClaudetteOutline
import com.olorin.claudette.ui.theme.ClaudettePrimary
import com.olorin.claudette.ui.theme.ClaudettePrimaryDark
import com.olorin.claudette.ui.theme.ClaudetteSurface
import com.olorin.claudette.ui.theme.ClaudetteSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    profileId: String,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    viewModel: ProfileEditorViewModel = hiltViewModel()
) {
    val saveResult by viewModel.saveResult.collectAsState()
    val context = LocalContext.current

    val isEditing = profileId != ProfileEditorViewModel.NEW_PROFILE_ID

    LaunchedEffect(profileId) {
        viewModel.loadProfile(profileId)
    }

    LaunchedEffect(saveResult) {
        when (saveResult) {
            is SaveResult.Success -> onSaved()
            is SaveResult.Error -> {
                Toast.makeText(
                    context,
                    (saveResult as SaveResult.Error).message,
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Profile" else "New Profile",
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancel",
                            tint = ClaudetteOnSurface
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveProfile() },
                        enabled = saveResult !is SaveResult.Saving
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = ClaudettePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save",
                            color = ClaudettePrimary,
                            fontFamily = FontFamily.Monospace
                        )
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionHeader(title = "Connection")

            ProfileTextField(
                value = viewModel.name,
                onValueChange = { viewModel.name = it },
                label = "Name",
                placeholder = "My Server"
            )

            ProfileTextField(
                value = viewModel.host,
                onValueChange = { viewModel.host = it },
                label = "Host",
                placeholder = "192.168.1.100"
            )

            ProfileTextField(
                value = viewModel.port.toString(),
                onValueChange = { newValue ->
                    viewModel.port = newValue.toIntOrNull() ?: viewModel.port
                },
                label = "Port",
                placeholder = "22",
                keyboardType = KeyboardType.Number
            )

            ProfileTextField(
                value = viewModel.username,
                onValueChange = { viewModel.username = it },
                label = "Username",
                placeholder = "user"
            )

            ProfileTextField(
                value = viewModel.projectPath,
                onValueChange = { viewModel.projectPath = it },
                label = "Default Project Path",
                placeholder = "/home/user/project"
            )

            ProfileTextField(
                value = viewModel.macAddress,
                onValueChange = { viewModel.macAddress = it },
                label = "MAC Address (for Wake-on-LAN)",
                placeholder = "AA:BB:CC:DD:EE:FF"
            )

            Spacer(modifier = Modifier.height(8.dp))

            SectionHeader(title = "Authentication")

            AuthMethodChips(
                selected = viewModel.authMethodSelection,
                onSelected = { viewModel.authMethodSelection = it }
            )

            Spacer(modifier = Modifier.height(4.dp))

            when (viewModel.authMethodSelection) {
                AuthMethodSelection.PASSWORD -> {
                    PasswordSection(
                        password = viewModel.password,
                        onPasswordChange = { viewModel.password = it }
                    )
                }
                AuthMethodSelection.GENERATED_KEY -> {
                    SshKeySection(
                        publicKey = viewModel.publicKey,
                        onGenerate = { viewModel.generateSshKey() },
                        generationType = "Generate"
                    )
                }
                AuthMethodSelection.IMPORTED_KEY -> {
                    SshKeySection(
                        publicKey = viewModel.publicKey,
                        onGenerate = null,
                        generationType = null
                    )
                    ImportKeySection(
                        onImport = { pem -> viewModel.importSshKey(pem) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader(title = "Bonjour Discovery")

            BonjourDiscoverySection(
                onHostSelected = { bonjourHost ->
                    viewModel.host = bonjourHost.hostname
                    viewModel.port = bonjourHost.port
                    if (viewModel.name.isBlank()) {
                        viewModel.name = bonjourHost.displayName
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = ClaudettePrimary,
        fontSize = 14.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(text = label, fontFamily = FontFamily.Monospace)
        },
        placeholder = {
            Text(text = placeholder, fontFamily = FontFamily.Monospace)
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ClaudettePrimary,
            unfocusedBorderColor = ClaudetteOutline,
            cursorColor = ClaudettePrimary,
            focusedLabelColor = ClaudettePrimary,
            unfocusedLabelColor = ClaudetteOutline,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedPlaceholderColor = ClaudetteOutline,
            unfocusedPlaceholderColor = ClaudetteOutline
        )
    )
}

@Composable
private fun AuthMethodChips(
    selected: AuthMethodSelection,
    onSelected: (AuthMethodSelection) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AuthMethodSelection.entries.forEach { method ->
            FilterChip(
                selected = selected == method,
                onClick = { onSelected(method) },
                label = {
                    Text(
                        text = when (method) {
                            AuthMethodSelection.PASSWORD -> "Password"
                            AuthMethodSelection.GENERATED_KEY -> "Generated Key"
                            AuthMethodSelection.IMPORTED_KEY -> "Imported Key"
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = when (method) {
                            AuthMethodSelection.PASSWORD -> Icons.Default.Key
                            AuthMethodSelection.GENERATED_KEY -> Icons.Default.VpnKey
                            AuthMethodSelection.IMPORTED_KEY -> Icons.Default.FileUpload
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ClaudettePrimaryDark,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White,
                    containerColor = ClaudetteSurfaceVariant,
                    labelColor = ClaudetteOnSurface,
                    iconColor = ClaudetteOnSurface
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = ClaudetteOutline,
                    selectedBorderColor = ClaudettePrimary,
                    enabled = true,
                    selected = selected == method
                )
            )
        }
    }
}

@Composable
private fun PasswordSection(
    password: String,
    onPasswordChange: (String) -> Unit
) {
    ProfileTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = "Password",
        placeholder = "Enter password",
        keyboardType = KeyboardType.Password,
        isPassword = true
    )
}

@Composable
private fun SshKeySection(
    publicKey: String?,
    onGenerate: (() -> Unit)?,
    generationType: String?
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ClaudetteSurfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (publicKey != null) {
                Text(
                    text = "Public Key",
                    color = ClaudettePrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = publicKey,
                    color = ClaudetteOnSurface,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ClaudetteSurface, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(publicKey))
                            Toast.makeText(context, "Public key copied", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ClaudettePrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Copy",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }

                    if (onGenerate != null && generationType != null) {
                        OutlinedButton(
                            onClick = onGenerate,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ClaudettePrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Regenerate",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "No SSH key configured",
                    color = ClaudetteOutline,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (onGenerate != null && generationType != null) {
                    Button(
                        onClick = onGenerate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClaudettePrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$generationType Ed25519 Key",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportKeySection(
    onImport: (String) -> Unit
) {
    var showImportDialog by remember { mutableStateOf(false) }
    var pemText by remember { mutableStateOf("") }

    Button(
        onClick = { showImportDialog = true },
        colors = ButtonDefaults.buttonColors(
            containerColor = ClaudettePrimary
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.FileUpload,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Import PEM Key",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp
        )
    }

    if (showImportDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = ClaudetteSurfaceVariant,
            title = {
                Text(
                    text = "Import SSH Key",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                OutlinedTextField(
                    value = pemText,
                    onValueChange = { pemText = it },
                    label = {
                        Text(
                            text = "PEM Private Key",
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    placeholder = {
                        Text(
                            text = "-----BEGIN PRIVATE KEY-----\n...",
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 20,
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
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onImport(pemText)
                        showImportDialog = false
                        pemText = ""
                    },
                    enabled = pemText.isNotBlank()
                ) {
                    Text(
                        text = "Import",
                        color = if (pemText.isNotBlank()) ClaudettePrimary else ClaudetteOutline,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    pemText = ""
                }) {
                    Text(
                        text = "Cancel",
                        color = ClaudetteOnSurface,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        )
    }
}

@Composable
private fun BonjourDiscoverySection(
    onHostSelected: (BonjourHost) -> Unit
) {
    // Bonjour/mDNS discovery uses Android NSD under the hood.
    // The discovered hosts are populated by the system NsdManager.
    // For now, display a placeholder card that will be connected
    // to the NSD discovery service when it is wired up.
    val discoveredHosts = remember { mutableStateOf<List<BonjourHost>>(emptyList()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ClaudetteSurfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (discoveredHosts.value.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = null,
                        tint = ClaudetteOutline,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Searching for nearby servers...",
                        color = ClaudetteOutline,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Text(
                    text = "Discovered Hosts",
                    color = ClaudettePrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                discoveredHosts.value.forEach { host ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onHostSelected(host) }
                            .background(ClaudetteSurface, RoundedCornerShape(4.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = ClaudettePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = host.displayName,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${host.hostname}:${host.port}",
                                color = ClaudetteOnSurface,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
