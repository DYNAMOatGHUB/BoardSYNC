package com.edge.smartboard.ui.capture

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edge.smartboard.models.RecordingState
import com.edge.smartboard.theme.*
import com.edge.smartboard.ui.dashboard.GlassCard
import com.edge.smartboard.ui.dashboard.SectionHeader
import com.edge.smartboard.viewmodel.CaptureViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CaptureScreen(
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val state   by viewModel.recordingState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ── POST_NOTIFICATIONS permission (Android 13+) ───────────────
    val notifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    // Request notification permission on first composition if needed
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            notifPermission?.status?.isGranted == false
        ) {
            notifPermission.launchPermissionRequest()
        }
    }

    // ── MediaProjection permission launcher ───────────────────
    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                viewModel.onProjectionGranted(result.resultCode, data)
                viewModel.startRecording()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Header ────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Videocam,
                contentDescription = null,
                tint   = NeonBlue,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Screen Recording",
                    style      = MaterialTheme.typography.headlineMedium,
                    color      = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Capture your screen as MP4 video",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ── Live Preview Panel ────────────────────────────────
        RecordingPreviewPanel(state)

        // ── Timer card ────────────────────────────────────────
        AnimatedVisibility(visible = state.isRecording) {
            RecordingTimerCard(state)
        }

        // ── Control buttons ───────────────────────────────────
        RecordingControlRow(
            state   = state,
            onStart = {
                val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                        as MediaProjectionManager
                projectionLauncher.launch(mgr.createScreenCaptureIntent())
            },
            onStop = viewModel::stopRecording
        )

        // ── Saved File Card ───────────────────────────────────
        AnimatedVisibility(
            visible = !state.isRecording && state.savedFilePath != null
        ) {
            state.savedFilePath?.let { path ->
                SavedFileCard(
                    path = path,
                    context = context,
                    durationSeconds = state.durationSeconds,
                    fileSizeBytes = state.fileSizeBytes,
                    onNewRecording = viewModel::resetSavedFile
                )
            }
        }

        // ── Info Section ──────────────────────────────────────
        SectionHeader("Recording Details")
        RecordingInfoCard()

        // ── How It Works ──────────────────────────────────────
        SectionHeader("How It Works")
        HowItWorksCard()

        Spacer(Modifier.height(80.dp))
    }
}

// ── Preview Panel ─────────────────────────────────────────────────

@Composable
fun RecordingPreviewPanel(state: RecordingState) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alpha"
    )
    val ringScale by rememberInfiniteTransition(label = "ring").animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "scale"
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        borderColor = if (state.isRecording) AccentRed else GlassBorder
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        if (state.isRecording)
                            listOf(AccentRed.copy(alpha = 0.08f), Color.Transparent)
                        else
                            listOf(NeonBlue.copy(alpha = 0.04f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Central icon with pulsing ring
                Box(contentAlignment = Alignment.Center) {
                    if (state.isRecording) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .scale(ringScale)
                                .clip(CircleShape)
                                .background(AccentRed.copy(alpha = 0.12f))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.isRecording)
                                    AccentRed.copy(alpha = 0.2f)
                                else
                                    NeonBlue.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (state.isRecording) Icons.Default.RadioButtonChecked
                            else Icons.Default.Videocam,
                            contentDescription = null,
                            tint = if (state.isRecording) AccentRed.copy(alpha = pulse) else TextDisabled,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (state.isRecording) {
                    Text(
                        "RECORDING",
                        color      = AccentRed.copy(alpha = pulse),
                        fontWeight = FontWeight.ExtraBold,
                        style      = MaterialTheme.typography.titleMedium,
                        letterSpacing = 3.sp
                    )
                    Text(
                        "Screen is being captured as MP4",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (state.savedFilePath != null) {
                    Text("Recording Saved", color = EmeraldGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Tap 'New Recording' to record again", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Ready to Record", color = TextDisabled, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
                    Text("Press START to begin screen recording", color = TextDisabled, style = MaterialTheme.typography.bodySmall)
                }
            }

            // LIVE / REC badge
            if (state.isRecording) {
                Box(modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentRed)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = pulse))
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "● REC",
                            color      = Color.White,
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                // File size indicator
                Box(modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CardDark.copy(alpha = 0.8f)
                    ) {
                        Text(
                            formatBytes(state.fileSizeBytes),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ── Timer Card ────────────────────────────────────────────────────

@Composable
fun RecordingTimerCard(state: RecordingState) {
    val h = state.durationSeconds / 3600
    val m = (state.durationSeconds % 3600) / 60
    val s = state.durationSeconds % 60

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = AccentRed.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "%02d:%02d:%02d".format(h, m, s),
                    style = MaterialTheme.typography.displaySmall,
                    color = AccentRed,
                    fontWeight = FontWeight.Bold
                )
                Text("Duration", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatBytes(state.fileSizeBytes),
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold
                )
                Text("File Size", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

// ── Control Row ───────────────────────────────────────────────────

@Composable
fun RecordingControlRow(
    state: RecordingState,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!state.isRecording) {
            GradientButton(
                text      = "START RECORDING",
                icon      = Icons.Default.RadioButtonChecked,
                gradient  = Brush.linearGradient(listOf(AccentRed, Color(0xFFB71C1C))),
                modifier  = Modifier.weight(1f),
                onClick   = onStart
            )
        } else {
            GradientButton(
                text    = "STOP & SAVE",
                icon    = Icons.Default.Stop,
                gradient = Brush.linearGradient(listOf(CardDarker, CardDark)),
                modifier = Modifier.weight(1f),
                onClick  = onStop
            )
        }
    }
}

// ── Saved File Card ───────────────────────────────────────────────

@Composable
fun SavedFileCard(
    path: String,
    context: android.content.Context,
    durationSeconds: Long,
    fileSizeBytes: Long,
    onNewRecording: () -> Unit
) {
    val fileExists = remember(path) { File(path).exists() }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (fileExists) EmeraldGreen else AccentRed
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            (if (fileExists) EmeraldGreen else AccentRed).copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background((if (fileExists) EmeraldGreen else AccentRed).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (fileExists) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (fileExists) EmeraldGreen else AccentRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (fileExists) "Recording Saved!" else "Recording Failed",
                        color = if (fileExists) EmeraldGreen else AccentRed,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (fileExists) "MP4 video ready" else "File not found — try again",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (fileExists) {
                HorizontalDivider(color = EmeraldGreen.copy(alpha = 0.2f))

                // Metadata rows
                StatRow("File", File(path).name)
                StatRow("Duration", formatDuration(durationSeconds), NeonBlue)
                StatRow("Size", formatBytes(fileSizeBytes), EmeraldGreen)
                StatRow("Format", "MP4 / H.264", TextSecondary)

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GradientButton(
                        text    = "Share",
                        icon    = Icons.Default.Share,
                        gradient = Brush.linearGradient(listOf(NeonBlue, NeonPurple)),
                        modifier = Modifier.weight(1f),
                        onClick = { shareVideo(context, path) }
                    )
                    GradientButton(
                        text    = "New Recording",
                        icon    = Icons.Default.Add,
                        gradient = Brush.linearGradient(listOf(EmeraldGreen, NeonBlue)),
                        modifier = Modifier.weight(1f),
                        onClick = onNewRecording
                    )
                }
            } else {
                GradientButton(
                    text    = "Try Again",
                    icon    = Icons.Default.Refresh,
                    gradient = Brush.linearGradient(listOf(AccentRed, NeonPurple)),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNewRecording
                )
            }
        }
    }
}

// ── Info Card ─────────────────────────────────────────────────────

@Composable
fun RecordingInfoCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InfoRow(Icons.Default.Hd, "Output Format", "MP4 (MPEG-4 container)")
            InfoRow(Icons.Default.VideoSettings, "Video Codec", "H.264 (AVC)")
            InfoRow(Icons.Default.Tune, "Bitrate", "5 Mbps")
            InfoRow(Icons.Default.Speed, "Frame Rate", "30 FPS")
            InfoRow(Icons.Default.AspectRatio, "Resolution", "Half screen (720p+)")
            InfoRow(Icons.Default.FolderOpen, "Saved To", "App external files/recordings/")
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = NeonBlue, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}

// ── How It Works ──────────────────────────────────────────────────

@Composable
fun HowItWorksCard() {
    val steps = listOf(
        Triple(Icons.Default.TouchApp,    "Tap START",       "Grant screen capture permission when prompted by Android"),
        Triple(Icons.Default.Videocam,    "Recording begins","Screen is captured as a high-quality MP4 video file"),
        Triple(Icons.Default.Stop,        "Tap STOP & SAVE", "Recording stops and the file is saved to device storage"),
        Triple(Icons.Default.Share,       "Share or Upload", "Share the MP4 or upload it for AI analysis")
    )
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            steps.forEachIndexed { idx, (icon, title, desc) ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = NeonBlue, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(desc, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        "${idx + 1}",
                        color = NeonBlue.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                if (idx < steps.size - 1) {
                    Box(
                        modifier = Modifier
                            .padding(start = 17.dp)
                            .width(2.dp)
                            .height(4.dp)
                            .background(NeonBlue.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

// ── Shared Composables ────────────────────────────────────────────

@Composable
fun GradientButton(
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
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun StatRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

// ── Helpers ───────────────────────────────────────────────────────

private fun formatBytes(bytes: Long): String = when {
    bytes <= 0       -> "0 B"
    bytes < 1024     -> "$bytes B"
    bytes < 1048576  -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1073741824L -> "%.1f MB".format(bytes / 1048576.0)
    else             -> "%.2f GB".format(bytes / 1073741824.0)
}

private fun formatDuration(s: Long) =
    "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)

private fun shareVideo(context: android.content.Context, path: String) {
    val file = File(path)
    if (!file.exists()) return
    val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type  = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Recording"))
}
