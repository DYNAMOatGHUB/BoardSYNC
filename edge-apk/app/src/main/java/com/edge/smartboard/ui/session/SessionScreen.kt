package com.edge.smartboard.ui.session

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
import com.edge.smartboard.ui.capture.StatRow
import com.edge.smartboard.ui.dashboard.GlassCard
import com.edge.smartboard.ui.dashboard.SectionHeader
import com.edge.smartboard.ui.reports.ActionButton
import com.edge.smartboard.viewmodel.SessionViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SessionScreen(
    sessionId: String,
    onUpload: (String) -> Unit,
    onReport: (String) -> Unit,
    onLive: (String) -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle(emptyList())
    val session  = sessions.find { it.sessionId == sessionId }

    if (session == null) {
        Box(Modifier.fillMaxSize().background(BackgroundDark), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NeonBlue)
        }
        return
    }

    val statusColor = when (session.status) {
        SessionStatus.COMPLETED  -> EmeraldGreen
        SessionStatus.UPLOADING  -> NeonBlue
        SessionStatus.PROCESSING -> AccentGold
        SessionStatus.FAILED     -> AccentRed
        SessionStatus.UPLOADED   -> NeonPurple
        else                     -> TextDisabled
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Session Details", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)

        // Session hero card
        GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = statusColor.copy(0.5f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(statusColor.copy(0.05f), Color.Transparent)))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonBlue.copy(0.3f), NeonPurple.copy(0.3f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(session.teacherName.take(2).uppercase(), color = NeonBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(session.teacherName, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("${session.subject} • ${session.classRoom}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(session.department, style = MaterialTheme.typography.bodySmall, color = TextDisabled)
                    }
                    Spacer(Modifier.weight(1f))
                    StatusBadge(session.status, statusColor)
                }
            }
        }

        // Session metadata
        SectionHeader("Session Info")
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatRow("Session ID",  session.sessionId)
                StatRow("Date",        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(session.date)))
                StatRow("Duration",    formatDuration(session.durationSeconds))
                StatRow("Images",      "${session.imageCount} frames")
                StatRow("Storage",     formatBytes(session.storageBytes))
                StatRow("Audio",       session.audioFile ?: "Not recorded")
                session.score?.let {
                    StatRow("AI Score", "%.1f%%".format(it), NeonBlue)
                }
            }
        }

        // Actions
        SectionHeader("Actions")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton(
                    text = "Upload",
                    icon = Icons.Default.CloudUpload,
                    gradient = Brush.linearGradient(listOf(NeonBlue, NeonPurple)),
                    modifier = Modifier.weight(1f)
                ) { onUpload(sessionId) }
                ActionButton(
                    text = "Live View",
                    icon = Icons.Default.PlayCircle,
                    gradient = Brush.linearGradient(listOf(EmeraldGreen, NeonBlue)),
                    modifier = Modifier.weight(1f)
                ) { onLive(sessionId) }
                ActionButton(
                    text = "Report",
                    icon = Icons.Default.Description,
                    gradient = Brush.linearGradient(listOf(NeonPurple, AccentOrange)),
                    modifier = Modifier.weight(1f)
                ) { onReport(sessionId) }
            }
            if (session.status == SessionStatus.FAILED) {
                ActionButton(
                    text = "Retry Upload",
                    icon = Icons.Default.Refresh,
                    gradient = Brush.linearGradient(listOf(AccentGold, AccentOrange)),
                    modifier = Modifier.fillMaxWidth()
                ) { onUpload(sessionId) }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun StatusBadge(status: SessionStatus, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(0.15f))
            .border(1.dp, color.copy(0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(status.name, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024       -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else               -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
}
