package com.edge.smartboard.ui.live

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import com.edge.smartboard.viewmodel.SessionViewModel

@Composable
fun LiveProcessScreen(
    sessionId: String,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val liveFrame by viewModel.liveFrame.collectAsStateWithLifecycle()

    LaunchedEffect(sessionId) {
        viewModel.startLiveProcessing(sessionId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Live AI Processing",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        liveFrame?.let { frame ->
            // Frame info header
            FrameHeaderCard(frame.frameIndex)

            // Current AI Agent
            SectionHeader("Active Agent")
            AgentIndicator(frame.currentAgent)

            // Transcript
            SectionHeader("Speech Transcript")
            TranscriptCard(frame.transcript, frame.speakerLabel)

            // Detected content
            SectionHeader("Visual Detection")
            DetectionCards(frame)

            // Detected text
            SectionHeader("Detected Text")
            GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = NeonBlue.copy(0.3f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(frame.detectedText, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(frame.visualContext, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            // Agent pipeline
            SectionHeader("AI Agent Pipeline")
            AgentPipelineCards(frame.currentAgent)

        } ?: run {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NeonBlue, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Initializing AI Pipeline...", color = TextSecondary)
                    Text("Waiting for first frame", color = TextDisabled, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun FrameHeaderCard(frameIndex: Int) {
    GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = NeonBlue.copy(0.4f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(NeonBlue.copy(0.05f), NeonPurple.copy(0.05f))))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Processing Frame", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                Text("#${frameIndex.toString().padStart(4, '0')}", style = MaterialTheme.typography.headlineSmall, color = NeonBlue, fontWeight = FontWeight.Bold)
            }
            val pulse by rememberInfiniteTransition(label = "fp").animateFloat(
                0.4f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), "alpha"
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(EmeraldGreen.copy(pulse)))
                Spacer(Modifier.width(6.dp))
                Text("LIVE", color = EmeraldGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun AgentIndicator(currentAgent: AgentType) {
    val (label, color, icon) = when (currentAgent) {
        AgentType.RETRIEVER      -> Triple("Retriever Agent",     NeonBlue,     Icons.Default.Search)
        AgentType.CONTENT_GRADER -> Triple("Content Grader",      EmeraldGreen, Icons.Default.Grading)
        AgentType.PEDAGOGY       -> Triple("Pedagogy Agent",      NeonPurple,   Icons.Default.School)
        AgentType.AUDITOR        -> Triple("Auditor Agent",        AccentGold,   Icons.Default.VerifiedUser)
        AgentType.REPORT_WRITER  -> Triple("Report Writer",        AccentOrange, Icons.Default.Description)
    }
    val pulse by rememberInfiniteTransition(label = "a").animateFloat(
        0.5f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), "alpha"
    )

    GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = color.copy(0.5f)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(0.05f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(color.copy(pulse * 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Currently Running", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(label, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            CircularProgressIndicator(color = color, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
fun TranscriptCard(transcript: String, speaker: String) {
    GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = EmeraldGreen.copy(0.3f)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.RecordVoiceOver, null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(speaker, style = MaterialTheme.typography.labelLarge, color = EmeraldGreen)
            }
            Spacer(Modifier.height(8.dp))
            AnimatedContent(
                targetState = transcript,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "transcript"
            ) { text ->
                Text(text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            }
        }
    }
}

@Composable
fun DetectionCards(frame: LiveFrame) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            DetectionChip("Diagram", frame.diagramDetected, Icons.Default.AccountTree)
        }
        item {
            DetectionChip("Math Formula", frame.mathDetected, Icons.Default.Calculate)
        }
        item {
            DetectionChip("Chart", frame.chartDetected, Icons.Default.BarChart)
        }
        item {
            DetectionChip("Whiteboard", frame.whiteboardDetected, Icons.Default.Dashboard)
        }
    }
}

@Composable
fun DetectionChip(label: String, detected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val color = if (detected) EmeraldGreen else TextDisabled
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(0.1f))
            .border(1.dp, color.copy(0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                Text(if (detected) "Detected" else "Not Found", style = MaterialTheme.typography.labelSmall, color = color.copy(0.6f))
            }
        }
    }
}

@Composable
fun AgentPipelineCards(currentAgent: AgentType) {
    val agents = listOf(
        AgentType.RETRIEVER,
        AgentType.CONTENT_GRADER,
        AgentType.PEDAGOGY,
        AgentType.AUDITOR,
        AgentType.REPORT_WRITER
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        agents.forEach { agent ->
            val isCurrent = agent == currentAgent
            val isDone    = agents.indexOf(agent) < agents.indexOf(currentAgent)
            val color     = when {
                isCurrent -> NeonBlue
                isDone    -> EmeraldGreen
                else      -> TextDisabled
            }
            val (label, icon) = when (agent) {
                AgentType.RETRIEVER      -> "Retriever Agent"     to Icons.Default.Search
                AgentType.CONTENT_GRADER -> "Content Grader"      to Icons.Default.Grading
                AgentType.PEDAGOGY       -> "Pedagogy Agent"      to Icons.Default.School
                AgentType.AUDITOR        -> "Auditor Agent"       to Icons.Default.VerifiedUser
                AgentType.REPORT_WRITER  -> "Report Writer"       to Icons.Default.Description
            }
            GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = color.copy(0.3f)) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(label, style = MaterialTheme.typography.titleSmall, color = color, modifier = Modifier.weight(1f))
                    Icon(
                        when {
                            isCurrent -> Icons.Default.PlayCircle
                            isDone    -> Icons.Default.CheckCircle
                            else      -> Icons.Default.RadioButtonUnchecked
                        },
                        null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
