package com.edge.smartboard.ui.reports

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
import com.edge.smartboard.models.Report
import com.edge.smartboard.theme.*
import com.edge.smartboard.ui.dashboard.GlassCard
import com.edge.smartboard.ui.dashboard.SectionHeader
import com.edge.smartboard.viewmodel.SessionViewModel

@Composable
fun ReportsScreen(
    sessionId: String,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val report by viewModel.report.collectAsStateWithLifecycle()

    LaunchedEffect(sessionId) {
        viewModel.loadReport(sessionId)
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
            "Teaching Report",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        report?.let { r ->
            // Overall score hero card
            OverallScoreCard(r.overallScore)

            // Score breakdown
            SectionHeader("Score Breakdown")
            ScoreBreakdownCard(r)

            // Strengths
            SectionHeader("Strengths")
            FeedbackList(r.strengths, EmeraldGreen, Icons.Default.ThumbUp)

            // Weaknesses
            SectionHeader("Areas for Improvement")
            FeedbackList(r.weaknesses, AccentOrange, Icons.Default.Warning)

            // Recommendations
            SectionHeader("Recommendations")
            FeedbackList(r.recommendations, NeonBlue, Icons.Default.Lightbulb)

            // Action buttons
            SectionHeader("Actions")
            ReportActions()
        } ?: run {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonBlue)
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun OverallScoreCard(score: Float) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = NeonBlue
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(NeonBlue.copy(alpha = 0.1f), NeonPurple.copy(alpha = 0.1f)))
                )
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Overall Teaching Score", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    Text(
                        "%.1f".format(score),
                        style = MaterialTheme.typography.displayMedium,
                        color = NeonBlue,
                        fontWeight = FontWeight.Bold
                    )
                    val grade = when {
                        score >= 90 -> "Excellent"
                        score >= 80 -> "Very Good"
                        score >= 70 -> "Good"
                        score >= 60 -> "Satisfactory"
                        else        -> "Needs Improvement"
                    }
                    Text(grade, style = MaterialTheme.typography.titleSmall, color = EmeraldGreen)
                }

                Box(contentAlignment = Alignment.Center) {
                    val animScore by animateFloatAsState(score / 100f, tween(1000), label = "score")
                    CircularProgressIndicator(
                        progress = { animScore },
                        modifier = Modifier.size(100.dp),
                        color = NeonBlue,
                        trackColor = NeonBlue.copy(alpha = 0.1f),
                        strokeWidth = 8.dp
                    )
                    Text(
                        "${score.toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreBreakdownCard(report: Report) {
    GlassCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val metrics = listOf(
                Triple("Lesson Coverage",     report.lessonCoverage,      NeonBlue),
                Triple("Student Engagement",  report.studentEngagement,   NeonPurple),
                Triple("Speaking Time",       report.speakingTime,        EmeraldGreen),
                Triple("Board Usage",         report.boardUsage,          AccentGold),
                Triple("Visual Teaching",     report.visualTeachingScore, NeonBlue),
                Triple("Voice Clarity",       report.voiceClarity,        EmeraldGreen),
                Triple("Curriculum Align.",   report.curriculumAlignment, NeonPurple),
            )
            metrics.forEach { (label, value, color) ->
                ScoreBar(label, value, color)
            }
        }
    }
}

@Composable
fun ScoreBar(label: String, value: Float, color: Color) {
    val animValue by animateFloatAsState(value / 100f, tween(800), label = "bar")
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text("%.1f%%".format(value), style = MaterialTheme.typography.labelLarge, color = color)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(CardDarker)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animValue)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.horizontalGradient(listOf(color.copy(0.5f), color)))
            )
        }
    }
}

@Composable
fun FeedbackList(
    items: List<String>,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            GlassCard(borderColor = color.copy(alpha = 0.3f)) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(item, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun ReportActions() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionButton(
            text = "Generate PDF",
            icon = Icons.Default.PictureAsPdf,
            gradient = Brush.linearGradient(listOf(AccentRed, Color(0xFF880E4F))),
            modifier = Modifier.weight(1f),
            onClick = {}
        )
        ActionButton(
            text = "Share",
            icon = Icons.Default.Share,
            gradient = Brush.linearGradient(listOf(NeonBlue, NeonPurple)),
            modifier = Modifier.weight(1f),
            onClick = {}
        )
        ActionButton(
            text = "Download",
            icon = Icons.Default.Download,
            gradient = Brush.linearGradient(listOf(EmeraldGreen, NeonBlue)),
            modifier = Modifier.weight(1f),
            onClick = {}
        )
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(gradient)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(text, color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}
