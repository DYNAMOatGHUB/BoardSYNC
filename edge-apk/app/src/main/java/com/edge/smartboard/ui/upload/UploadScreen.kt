package com.edge.smartboard.ui.upload

import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edge.smartboard.theme.*
import com.edge.smartboard.ui.dashboard.GlassCard
import com.edge.smartboard.ui.dashboard.SectionHeader
import com.edge.smartboard.viewmodel.SessionViewModel

@Composable
fun UploadScreen(
    sessionId: String,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
    val sessions    by viewModel.sessions.collectAsStateWithLifecycle(emptyList())
    val session     = sessions.find { it.sessionId == sessionId }

    val animProgress by animateFloatAsState(
        targetValue = uploadState.progressPercent / 100f,
        animationSpec = tween(300),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Upload Session",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        // Upload progress circle
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = if (uploadState.isUploading) NeonBlue else GlassBorder
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { animProgress },
                        modifier = Modifier.size(140.dp),
                        color = NeonBlue,
                        trackColor = NeonBlue.copy(alpha = 0.1f),
                        strokeWidth = 10.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${uploadState.progressPercent}%",
                            style = MaterialTheme.typography.displaySmall,
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (uploadState.progressPercent == 100) "Complete!" else "Uploading...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                if (uploadState.isUploading) {
                    // Upload stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        UploadStat("Speed", "%.0f KB/s".format(uploadState.speedKBps))
                        UploadStat("Remaining", "${uploadState.remainingSeconds}s")
                        UploadStat("Status", "Uploading")
                    }
                }
            }
        }

        // Package contents
        SectionHeader("Package Contents")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PackageItem(Icons.Default.Image, "Unique JPEG Frames", "${session?.imageCount ?: 0} frames", EmeraldGreen)
                PackageItem(Icons.Default.AudioFile, "Audio WAV", "16kHz Mono", NeonBlue)
                PackageItem(Icons.Default.DataObject, "Metadata JSON", "Session info + timestamps", NeonPurple)
                PackageItem(Icons.Default.FolderZip, "ZIP Package", formatBytes(session?.storageBytes ?: 0), AccentGold)
            }
        }

        // Upload controls
        SectionHeader("Controls")
        if (!uploadState.isUploading) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                session?.let {
                    UploadActionButton(
                        text = "Start Upload",
                        icon = Icons.Default.CloudUpload,
                        gradient = Brush.linearGradient(listOf(NeonBlue, NeonPurple)),
                        modifier = Modifier.weight(1f)
                    ) { viewModel.startUpload(it) }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                UploadActionButton(
                    text = "Retry",
                    icon = Icons.Default.Refresh,
                    gradient = Brush.linearGradient(listOf(AccentGold, AccentOrange)),
                    modifier = Modifier.weight(1f)
                ) { session?.let { viewModel.retryUpload(it) } }
                UploadActionButton(
                    text = "Cancel",
                    icon = Icons.Default.Cancel,
                    gradient = Brush.linearGradient(listOf(AccentRed, Color(0xFF880E4F))),
                    modifier = Modifier.weight(1f)
                ) { viewModel.cancelUpload() }
            }
        }

        // Background upload info
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudSync, null, tint = NeonBlue, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Background Upload", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("WorkManager ensures upload continues even if app is closed", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun UploadStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = NeonBlue, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun PackageItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    detail: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
fun UploadActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024       -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else               -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    }
}
