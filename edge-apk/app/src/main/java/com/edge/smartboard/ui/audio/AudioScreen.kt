package com.edge.smartboard.ui.audio

import android.Manifest
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edge.smartboard.theme.*
import com.edge.smartboard.ui.capture.GradientButton
import com.edge.smartboard.ui.capture.StatRow
import com.edge.smartboard.ui.dashboard.GlassCard
import com.edge.smartboard.ui.dashboard.SectionHeader
import com.edge.smartboard.viewmodel.AudioViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AudioScreen(
    viewModel: AudioViewModel = hiltViewModel()
) {
    val state by viewModel.audioState.collectAsStateWithLifecycle()

    val hours   = state.durationMs / 3_600_000
    val minutes = (state.durationMs % 3_600_000) / 60_000
    val secs    = (state.durationMs % 60_000) / 1000

    // ── RECORD_AUDIO permission ───────────────────────────────────
    val audioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Audio Recording",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )

        // ── Permission banner ─────────────────────────────────────
        if (!audioPermission.status.isGranted) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = AccentGold
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MicOff,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Microphone Permission Required",
                                style = MaterialTheme.typography.titleSmall,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (audioPermission.status.shouldShowRationale)
                                    "Audio recording needs microphone access. Please grant it."
                                else
                                    "Grant microphone permission to enable audio recording.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = { audioPermission.launchPermissionRequest() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                    ) {
                        Icon(Icons.Default.Mic, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Grant Microphone Permission", color = Color.White)
                    }
                }
            }
        }

        // Waveform visualizer
        SectionHeader("Audio Waveform")
        WaveformVisualizer(
            levels = state.audioLevels,
            isRecording = state.isRecording && !state.isPaused
        )

        // Timer + Status
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = if (state.isRecording) AccentRed else GlassBorder
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "%02d:%02d:%02d".format(hours, minutes, secs),
                        style = MaterialTheme.typography.displaySmall,
                        color = if (state.isRecording) AccentRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Recording Timer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MicrophoneStatusIcon(isRecording = state.isRecording, isPaused = state.isPaused)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        when {
                            state.isRecording && !state.isPaused -> "RECORDING"
                            state.isPaused -> "PAUSED"
                            else -> "IDLE"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            state.isRecording && !state.isPaused -> AccentRed
                            state.isPaused -> AccentGold
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Audio level meter
        SectionHeader("Audio Level")
        AudioLevelMeter(levels = state.audioLevels)

        // Controls — only show full controls when permission is granted
        SectionHeader("Controls")
        AudioControls(
            isRecording         = state.isRecording,
            isPaused            = state.isPaused,
            hasPermission       = audioPermission.status.isGranted,
            onRequestPermission = { audioPermission.launchPermissionRequest() },
            onStart             = viewModel::startRecording,
            onPause             = viewModel::pauseRecording,
            onResume            = viewModel::resumeRecording,
            onStop              = viewModel::stopRecording,
            onSave              = viewModel::saveWav
        )

        // Saved file info
        state.savedFilePath?.let { path ->
            val isError = path.startsWith("ERROR")
            GlassCard(borderColor = if (isError) AccentRed else EmeraldGreen) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isError) Icons.Default.Error else Icons.Default.AudioFile,
                        null,
                        tint = if (isError) AccentRed else EmeraldGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (isError) "Recording Error" else "WAV Saved",
                            color = if (isError) AccentRed else EmeraldGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            path,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Info
        GlassCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatRow("Format", "16kHz Mono WAV")
                StatRow("Bit Depth", "16-bit PCM")
                StatRow("Encoder", "MediaRecorder / AudioRecord")
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun WaveformVisualizer(levels: List<Float>, isRecording: Boolean) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        borderColor = if (isRecording) AccentRed.copy(alpha = 0.5f) else GlassBorder
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (levels.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    levels.forEach { level ->
                        val height by animateFloatAsState(
                            targetValue = (level * 80).coerceAtLeast(4f),
                            animationSpec = tween(80),
                            label = "wave"
                        )
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(height.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(AccentRed, NeonPurple)
                                    )
                                )
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(40) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(TextDisabled)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioLevelMeter(levels: List<Float>) {
    val avg = levels.takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 0f
    val animLevel by animateFloatAsState(avg, tween(200), label = "level")

    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Input Level", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${(avg * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animLevel)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(EmeraldGreen, AccentGold, AccentRed)
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun MicrophoneStatusIcon(isRecording: Boolean, isPaused: Boolean) {
    val pulse by rememberInfiniteTransition(label = "mic").animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "alpha"
    )
    val color = when {
        isRecording && !isPaused -> AccentRed
        isPaused -> AccentGold
        else -> TextDisabled
    }
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = if (isRecording && !isPaused) pulse * 0.3f else 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Mic, null, tint = color, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun AudioControls(
    isRecording: Boolean,
    isPaused: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSave: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isRecording) {
                GradientButton(
                    text = if (hasPermission) "RECORD" else "GRANT MIC",
                    icon = if (hasPermission) Icons.Default.FiberManualRecord else Icons.Default.Mic,
                    gradient = Brush.linearGradient(
                        if (hasPermission) listOf(AccentRed, Color(0xFF880E4F))
                        else listOf(AccentGold, AccentOrange)
                    ),
                    modifier = Modifier.weight(1f),
                    onClick  = if (hasPermission) onStart else onRequestPermission
                )
            } else {
                if (!isPaused) {
                    GradientButton(
                        text = "PAUSE",
                        icon = Icons.Default.Pause,
                        gradient = Brush.linearGradient(listOf(AccentGold, AccentOrange)),
                        modifier = Modifier.weight(1f),
                        onClick = onPause
                    )
                } else {
                    GradientButton(
                        text = "RESUME",
                        icon = Icons.Default.PlayArrow,
                        gradient = Brush.linearGradient(listOf(EmeraldGreen, NeonBlue)),
                        modifier = Modifier.weight(1f),
                        onClick = onResume
                    )
                }
                GradientButton(
                    text = "STOP",
                    icon = Icons.Default.Stop,
                    gradient = Brush.linearGradient(listOf(TextDisabled, CardDark)),
                    modifier = Modifier.weight(1f),
                    onClick = onStop
                )
            }
        }
        if (!isRecording) {
            GradientButton(
                text = "SAVE WAV",
                icon = Icons.Default.Save,
                gradient = Brush.linearGradient(listOf(NeonBlue, NeonPurple)),
                modifier = Modifier.fillMaxWidth(),
                onClick = onSave
            )
        }
    }
}
