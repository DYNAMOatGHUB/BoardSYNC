package com.edge.smartboard.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edge.smartboard.models.*
import com.edge.smartboard.theme.*
import com.edge.smartboard.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val dashState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val metrics   by viewModel.systemMetrics.collectAsStateWithLifecycle()
    val admin     by viewModel.adminDashboard.collectAsStateWithLifecycle()
    val notifs    by viewModel.notifications.collectAsStateWithLifecycle()

    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(1000L)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DashboardTopBar(
                schoolName  = dashState.schoolName,
                deviceName  = dashState.deviceName,
                time        = currentTime,
                notifCount  = dashState.notifications,
                onNotif     = { onNavigate("notifications") },
                onProfile   = { onNavigate("settings") }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item { SectionHeader("Live Status") }
            item { StatusCardsRow(dashState, metrics) }

            item { SectionHeader("System Queues") }
            item { QueueStatusSection(metrics.queues) }

            item { SectionHeader("Today's Overview") }
            item { AdminOverviewCards(admin) }

            item { SectionHeader("Weekly Performance") }
            item { WeeklyBarChart(admin.weeklyData) }

            item { SectionHeader("Recent Notifications") }
            item { NotificationsPreview(notifs.take(3)) }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    schoolName: String,
    deviceName: String,
    time: String,
    notifCount: Int,
    onNotif: () -> Unit,
    onProfile: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // School info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schoolName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            // Time display
            GlassCard(modifier = Modifier.padding(horizontal = 8.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Notifications
            BadgedBox(
                badge = {
                    if (notifCount > 0) Badge { Text("$notifCount") }
                }
            ) {
                IconButton(onClick = onNotif) {
                    Icon(Icons.Default.Notifications, null, tint = TextSecondary)
                }
            }

            // Profile
            IconButton(onClick = onProfile) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(NeonBlue, NeonPurple))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        // Bottom divider line
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
fun StatusCardsRow(dashState: DashboardState, metrics: SystemMetrics) {
    val items = listOf(
        Triple("Screen Recording", dashState.screenCaptureActive, Icons.Default.Videocam),
        Triple("Audio Recording", dashState.audioRecordingActive, Icons.Default.Mic),
        Triple("AI Processing",  dashState.aiProcessingActive,  Icons.Default.Psychology),
        Triple("Upload Queue",   dashState.uploadQueueSize > 0,  Icons.Default.CloudUpload),
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items) { (name, active, icon) ->
            StatusCard(name, active, icon)
        }
        item {
            GpuCard(metrics.gpuPercent)
        }
        item {
            NetworkCard(dashState.networkHealthMs)
        }
        item {
            SessionDurationCard(dashState.sessionDurationSeconds)
        }
    }
}

@Composable
fun StatusCard(name: String, active: Boolean, icon: ImageVector) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "alpha"
    )
    val color = if (active) EmeraldGreen else TextDisabled

    GlassCard(
        modifier = Modifier.width(140.dp),
        borderColor = if (active) EmeraldGreen else GlassBorder
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = if (active) pulse else 1f))
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (active) "ACTIVE" else "IDLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GpuCard(gpuPercent: Float) {
    GlassCard(modifier = Modifier.width(140.dp), borderColor = NeonPurple) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GPU Status", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator(
                progress = { gpuPercent / 100f },
                color = NeonPurple,
                trackColor = NeonPurple.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            Spacer(Modifier.height(4.dp))
            Text("${gpuPercent.toInt()}%", style = MaterialTheme.typography.labelLarge, color = NeonPurple)
        }
    }
}

@Composable
fun NetworkCard(latencyMs: Long) {
    val color = when {
        latencyMs < 30  -> EmeraldGreen
        latencyMs < 80  -> AccentGold
        else            -> AccentRed
    }
    GlassCard(modifier = Modifier.width(140.dp), borderColor = color) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Wifi, null, tint = color, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text("Network", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text("${latencyMs}ms", style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SessionDurationCard(durationSeconds: Long) {
    val hours   = durationSeconds / 3600
    val minutes = (durationSeconds % 3600) / 60
    val secs    = durationSeconds % 60
    GlassCard(modifier = Modifier.width(140.dp), borderColor = NeonBlue) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Timer, null, tint = NeonBlue, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text("Session", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
                "%02d:%02d:%02d".format(hours, minutes, secs),
                style = MaterialTheme.typography.titleSmall,
                color = NeonBlue,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun QueueStatusSection(queues: List<QueueStatus>) {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            queues.forEach { queue ->
                QueueRow(queue)
            }
        }
    }
}

@Composable
fun QueueRow(queue: QueueStatus) {
    val color = when (queue.status) {
        PipelineStatus.PROCESSING -> NeonBlue
        PipelineStatus.COMPLETED  -> EmeraldGreen
        PipelineStatus.FAILED     -> AccentRed
        else                      -> TextDisabled
    }
    val animProgress by animateFloatAsState(
        targetValue = (queue.length / 15f).coerceIn(0f, 1f),
        animationSpec = tween(800),
        label = "queue"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(8.dp))
                Text(queue.name, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Text(
                "${queue.length} jobs",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
        Spacer(Modifier.height(4.dp))
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
                        Brush.horizontalGradient(listOf(color.copy(alpha = 0.5f), color))
                    )
            )
        }
    }
}

@Composable
fun AdminOverviewCards(admin: AdminDashboard) {
    val metrics = listOf(
        Triple("Total Sessions",   "${admin.totalSessions}",       Icons.Default.VideoLibrary),
        Triple("Today's Sessions", "${admin.todaySessions}",       Icons.Default.Today),
        Triple("Avg Score",        "${admin.avgTeachingScore}%",   Icons.Default.Stars),
        Triple("Upload Success",   "${admin.uploadSuccessRate}%",  Icons.Default.CloudDone),
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(metrics) { (label, value, icon) ->
            MetricCard(label, value, icon)
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, icon: ImageVector) {
    GlassCard(modifier = Modifier.width(160.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = NeonBlue, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun WeeklyBarChart(data: List<Float>) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val max  = data.maxOrNull() ?: 100f

    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEachIndexed { i, value ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val animH by animateFloatAsState(
                            targetValue = (value / max),
                            animationSpec = tween(800, delayMillis = i * 80),
                            label = "bar"
                        )
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height((120 * animH).dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    Brush.verticalGradient(listOf(NeonBlue, NeonPurple))
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(days.getOrElse(i) { "" }, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsPreview(notifications: List<AppNotification>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        notifications.forEach { notif ->
            NotificationRow(notif)
        }
    }
}

@Composable
fun NotificationRow(notif: AppNotification) {
    val color = when (notif.type) {
        NotificationType.AI_FINISHED    -> EmeraldGreen
        NotificationType.REPORT_READY   -> NeonBlue
        NotificationType.UPLOAD_FAILED  -> AccentRed
        NotificationType.NETWORK_LOST   -> AccentOrange
        else                            -> TextSecondary
    }
    GlassCard(borderColor = if (!notif.isRead) color else GlassBorder) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (!notif.isRead) color else Color.Transparent)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(notif.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text(notif.message, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(NeonBlue)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Unspecified,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val border = if (borderColor == Color.Unspecified) scheme.outlineVariant else borderColor
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceVariant,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, border)
    ) {
        content()
    }
}
