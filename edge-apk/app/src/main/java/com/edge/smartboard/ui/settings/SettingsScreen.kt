package com.edge.smartboard.ui.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edge.smartboard.theme.*
import com.edge.smartboard.theme.LocalThemeManager
import com.edge.smartboard.theme.ThemeMode
import com.edge.smartboard.ui.dashboard.GlassCard
import com.edge.smartboard.ui.dashboard.SectionHeader
import com.edge.smartboard.viewmodel.AuthViewModel

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val savedUrl by authViewModel.serverUrl.collectAsStateWithLifecycle()
    var serverUrl       by remember(savedUrl) { mutableStateOf(savedUrl) }
    var apiKey          by remember { mutableStateOf("") }
    var captureFps      by remember { mutableStateOf(1f) }
    var captureRes      by remember { mutableStateOf("1080p") }
    var autoUpload      by remember { mutableStateOf(true) }
    var backgroundSync  by remember { mutableStateOf(true) }
    var showAbout       by remember { mutableStateOf(false) }

    val themeManager   = LocalThemeManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        // Profile card
        ProfileCard()

        // Server config
        SectionHeader("Server Configuration")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("Server URL") },
                    leadingIcon = { Icon(Icons.Default.Dns, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { authViewModel.saveServerUrl(serverUrl) }) {
                            Icon(Icons.Default.Save, "Save URL", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor     = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor   = MaterialTheme.colorScheme.onSurface,
                        cursorColor          = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    "Tap 💾 to save. Changes take effect on next request.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Capture settings
        SectionHeader("Capture Settings")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Frame Rate: ${captureFps.toInt()} FPS", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Slider(
                    value = captureFps,
                    onValueChange = { captureFps = it },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonBlue,
                        activeTrackColor = NeonBlue,
                        inactiveTrackColor = CardDarker
                    )
                )
                // Capture resolution
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("720p", "1080p", "4K").forEach { res ->
                        FilterChip(
                            selected = captureRes == res,
                            onClick  = { captureRes = res },
                            label    = { Text(res) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonBlue,
                                selectedLabelColor = TextOnNeon
                            )
                        )
                    }
                }
            }
        }

        // Theme
        SectionHeader("Appearance")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Theme", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Dark"  to ThemeMode.DARK,
                        "Light" to ThemeMode.LIGHT,
                        "Auto"  to ThemeMode.AUTO
                    ).forEach { (label, mode) ->
                        FilterChip(
                            selected = themeManager.mode == mode,
                            onClick  = { themeManager.mode = mode },
                            label    = { Text(label) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonPurple,
                                selectedLabelColor = TextPrimary
                            )
                        )
                    }
                }
            }
        }

        // Sync options
        SectionHeader("Sync & Upload")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(4.dp)) {
                ToggleRow("Auto Upload", "Automatically upload completed sessions", autoUpload) { autoUpload = it }
                HorizontalDivider(color = GlassBorder, thickness = 1.dp)
                ToggleRow("Background Sync", "Sync in background when on Wi-Fi", backgroundSync) { backgroundSync = it }
            }
        }

        // Storage
        SectionHeader("Storage")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsRow(Icons.Default.Storage, "Storage Manager", "View and manage stored sessions", NeonBlue) {}
                SettingsRow(Icons.Default.DeleteSweep, "Delete All Sessions", "Clear all local session data", AccentRed) {}
            }
        }

        // About
        SectionHeader("About")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsRow(Icons.Default.Info, "About Edge Smartboard AI", "Version 1.0.0 • Enterprise Edition", NeonBlue) { showAbout = !showAbout }
                if (showAbout) {
                    Column(modifier = Modifier.padding(start = 32.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Edge Smartboard AI", color = NeonBlue, fontWeight = FontWeight.Bold)
                        Text("Built with Jetpack Compose + MVVM", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text("CameraX • MediaProjection • WorkManager", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text("Hilt • Room • Retrofit • ExoPlayer", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Logout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AccentRed.copy(alpha = 0.1f))
                .border(1.dp, AccentRed.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .clickable { onLogout() }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Logout, null, tint = AccentRed, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", color = AccentRed, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun ProfileCard() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = NeonBlue.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(NeonBlue.copy(0.05f), NeonPurple.copy(0.05f))))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonBlue, NeonPurple))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Admin User", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("admin@school.edu", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonPurple.copy(0.2f))
                        .border(1.dp, NeonPurple.copy(0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("ADMIN", style = MaterialTheme.typography.labelSmall, color = NeonPurple, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonBlue,
                checkedTrackColor = NeonBlue.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color = NeonBlue,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextDisabled)
    }
}

@Composable
fun neonTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NeonBlue,
    unfocusedBorderColor = GlassBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = NeonBlue,
    focusedContainerColor = GlassWhite,
    unfocusedContainerColor = GlassWhite
)
