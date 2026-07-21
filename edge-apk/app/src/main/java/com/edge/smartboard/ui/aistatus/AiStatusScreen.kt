package com.edge.smartboard.ui.aistatus

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edge.smartboard.models.*
import com.edge.smartboard.theme.*
import com.edge.smartboard.ui.dashboard.GlassCard
import com.edge.smartboard.ui.dashboard.SectionHeader
import com.edge.smartboard.viewmodel.DashboardViewModel

@Composable
fun AiStatusScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val metrics by viewModel.systemMetrics.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "AI Pipeline Status",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        // CPU + GPU Overview
        SectionHeader("System Resources")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ResourceGauge("CPU", metrics.cpuPercent, NeonBlue, modifier = Modifier.weight(1f))
            ResourceGauge("GPU", metrics.gpuPercent, NeonPurple, modifier = Modifier.weight(1f))
            GpuMemoryCard(metrics.gpuMemoryMb, modifier = Modifier.weight(1f))
        }

        // Queue status
        SectionHeader("Processing Queues")
        metrics.queues.forEach { queue ->
            QueueCard(queue)
        }

        // Live pipeline visualizer
        SectionHeader("Pipeline Flow")
        PipelineVisualizer(metrics.queues)

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun ResourceGauge(label: String, percent: Float, color: Color, modifier: Modifier = Modifier) {
    val animPercent by animateFloatAsState(percent / 100f, tween(800), label = "gauge")

    GlassCard(modifier = modifier, borderColor = color.copy(alpha = 0.4f)) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animPercent },
                    modifier = Modifier.size(72.dp),
                    color = color,
                    trackColor = color.copy(alpha = 0.1f),
                    strokeWidth = 6.dp
                )
                Text(
                    "${percent.toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Text(
                when {
                    percent < 30 -> "Low Load"
                    percent < 70 -> "Moderate"
                    else         -> "High Load"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    percent < 30 -> EmeraldGreen
                    percent < 70 -> AccentGold
                    else         -> AccentRed
                }
            )
        }
    }
}

@Composable
fun GpuMemoryCard(memoryMb: Int, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, borderColor = NeonBlue.copy(alpha = 0.4f)) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Memory, null, tint = NeonBlue, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                "${memoryMb / 1024}GB",
                style = MaterialTheme.typography.titleLarge,
                color = NeonBlue,
                fontWeight = FontWeight.Bold
            )
            Text("GPU VRAM", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text("Used", style = MaterialTheme.typography.labelSmall, color = TextDisabled)
        }
    }
}

@Composable
fun QueueCard(queue: QueueStatus) {
    val statusColor = when (queue.status) {
        PipelineStatus.PROCESSING -> NeonBlue
        PipelineStatus.COMPLETED  -> EmeraldGreen
        PipelineStatus.FAILED     -> AccentRed
        else                      -> TextDisabled
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = statusColor.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (queue.status == PipelineStatus.PROCESSING) {
                    CircularProgressIndicator(
                        color = statusColor,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        when (queue.status) {
                            PipelineStatus.COMPLETED -> Icons.Default.CheckCircle
                            PipelineStatus.FAILED    -> Icons.Default.Error
                            else                     -> Icons.Default.HourglassEmpty
                        },
                        null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(queue.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(
                    "${queue.length} job(s) queued • ${queue.latencyMs}ms avg",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(6.dp))
                val animProgress by animateFloatAsState(
                    targetValue = (queue.length / 15f).coerceIn(0f, 1f),
                    animationSpec = tween(600),
                    label = "q"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(CardDarker)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(listOf(statusColor.copy(0.5f), statusColor))
                            )
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    queue.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PipelineVisualizer(queues: List<QueueStatus>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "End-to-End Pipeline",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val stages = listOf("Input", "Vision", "Audio", "Reason", "Output")
                stages.forEachIndexed { i, stage ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val color = when (i) {
                            0 -> NeonBlue
                            1 -> NeonPurple
                            2 -> AccentGold
                            3 -> EmeraldGreen
                            else -> NeonBlue
                        }
                        val pulse by rememberInfiniteTransition(label = "p$i").animateFloat(
                            initialValue = 0.6f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(1000, delayMillis = i * 200), RepeatMode.Reverse),
                            label = "a"
                        )
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color.copy(alpha = pulse * 0.3f))
                                .border(2.dp, color, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(stage, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }

                    if (i < stages.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(2.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(NeonBlue.copy(0.5f), NeonPurple.copy(0.5f))
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}
