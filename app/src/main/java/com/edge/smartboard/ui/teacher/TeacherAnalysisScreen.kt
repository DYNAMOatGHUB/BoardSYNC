package com.edge.smartboard.ui.teacher

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edge.smartboard.theme.*
import com.edge.smartboard.ui.dashboard.GlassCard
import com.edge.smartboard.ui.dashboard.SectionHeader
import com.edge.smartboard.viewmodel.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TeacherAnalysisScreen(
    onBack: () -> Unit,
    viewModel: TeacherAnalysisViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        if (state.selectedTeacher == null) {
            TeacherListView(
                state    = state,
                onSelect = viewModel::selectTeacher,
                onAdd    = { viewModel.setShowAddTeacher(true) },
                onBack   = onBack
            )
        } else {
            TeacherDetailView(
                state    = state,
                onUpload = { file -> viewModel.uploadRecording(state.selectedTeacher!!, file) },
                onBack   = viewModel::clearSelection
            )
        }

        // Add-teacher dialog
        if (state.showAddTeacher) {
            AddTeacherDialog(
                onConfirm = viewModel::addTeacher,
                onDismiss = { viewModel.setShowAddTeacher(false) }
            )
        }
    }
}

// ─── Teacher list ──────────────────────────────────────────
@Composable
private fun TeacherListView(
    state: TeacherAnalysisUiState,
    onSelect: (TeacherProfile) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = TextSecondary)
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Teacher Analysis", style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("Upload & analyse teaching recordings", style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(NeonBlue, NeonPurple)))
                    .clickable { onAdd() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add", color = Color.White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Analysis level legend
        SectionHeader("Analysis Tiers")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(AnalysisLevel.values()) { level ->
                LevelBadge(level)
            }
        }

        // Teacher cards
        SectionHeader("Teachers (${state.teachers.size})")
        state.teachers.forEach { teacher ->
            TeacherCard(teacher = teacher, onClick = { onSelect(teacher) })
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun LevelBadge(level: AnalysisLevel) {
    val color = Color(level.color)
    GlassCard(borderColor = color.copy(0.4f)) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(level.label, style = MaterialTheme.typography.labelMedium,
                color = color, fontWeight = FontWeight.Bold)
            Text("${level.minUploads}+ uploads", style = MaterialTheme.typography.labelSmall,
                color = TextSecondary)
        }
    }
}

@Composable
private fun TeacherCard(teacher: TeacherProfile, onClick: () -> Unit) {
    val levelColor = Color(teacher.analysisLevel.color)
    val pulse by rememberInfiniteTransition(label = "p").animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "a"
    )

    GlassCard(
        modifier    = Modifier.fillMaxWidth().clickable { onClick() },
        borderColor = levelColor.copy(0.3f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(levelColor.copy(0.3f), levelColor.copy(0.6f)))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    teacher.name.split(" ").take(2).joinToString("") { it.first().toString() },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(teacher.name, style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text("${teacher.subject} · ${teacher.department}",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("${teacher.uploadCount}", "uploads", NeonBlue)
                    if (teacher.uploadCount > 0)
                        MiniStat("%.1f%%".format(teacher.avgScore), "avg score", EmeraldGreen)
                }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Level chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(levelColor.copy(alpha = pulse * 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(teacher.analysisLevel.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = levelColor, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text("$value $label", style = MaterialTheme.typography.labelSmall, color = color)
    }
}

// ─── Teacher detail + upload ───────────────────────────────
@Composable
private fun TeacherDetailView(
    state: TeacherAnalysisUiState,
    onUpload: (File) -> Unit,
    onBack: () -> Unit
) {
    val teacher    = state.selectedTeacher ?: return
    val levelColor = Color(teacher.analysisLevel.color)
    val context    = LocalContext.current

    // Real video file picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val videoFile = copyUriToCache(context, it)
            if (videoFile != null) onUpload(videoFile)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back + name
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = TextSecondary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(teacher.name, style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("${teacher.subject} · ${teacher.department}",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            // Upload button — launches system video picker
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (!state.isUploading && !state.isAnalyzing)
                            Brush.linearGradient(listOf(EmeraldGreen, NeonBlue))
                        else
                            Brush.linearGradient(listOf(TextDisabled, TextDisabled))
                    )
                    .clickable(enabled = !state.isUploading && !state.isAnalyzing) {
                        videoPickerLauncher.launch("video/*")
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (state.isUploading || state.isAnalyzing) "Processing…" else "Upload Video",
                        color = Color.White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Upload / analysis in-progress
        AnimatedVisibility(visible = state.isUploading || state.isAnalyzing) {
            ProgressCard(state)
        }

        // Level + next-level progress
        SectionHeader("Analysis Level")
        LevelProgressCard(teacher, levelColor)

        // Score summary
        state.latestReport?.let { report ->
            if (report.totalRecordings > 0) {
                SectionHeader("Performance Summary")
                PerformanceSummaryCard(report)

                SectionHeader("Recommendations")
                RecommendationsCard(report)
            }
        }

        // Recording history
        SectionHeader("Recording History (${teacher.uploadCount})")
        if (state.recordings.isEmpty()) {
            EmptyRecordingsCard()
        } else {
            state.recordings.forEach { rec ->
                RecordingRow(rec)
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun ProgressCard(state: TeacherAnalysisUiState) {
    val isUploading = state.isUploading
    val progress = if (isUploading) state.uploadProgress else state.analysisProgress
    val label = if (isUploading) "Uploading recording…" else state.analysisStage
    val color = if (isUploading) NeonBlue else NeonPurple
    val animP by animateFloatAsState(progress / 100f, tween(300), label = "p")

    GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = color.copy(0.5f)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    progress = { animP },
                    color = color,
                    trackColor = color.copy(0.1f),
                    strokeWidth = 6.dp,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(label, style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("$progress%", style = MaterialTheme.typography.bodySmall, color = color)
                    if (isUploading) {
                        Text("%.0f KB/s".format(state.uploadSpeedKBps),
                            style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
            LinearProgressIndicator(
                progress = { animP },
                color = color,
                trackColor = color.copy(0.1f),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun LevelProgressCard(teacher: TeacherProfile, levelColor: Color) {
    val nextLevel = AnalysisLevel.values().firstOrNull { it.minUploads > teacher.uploadCount }
    val progressToNext = if (nextLevel != null) {
        val prev = AnalysisLevel.values().lastOrNull { it.minUploads <= teacher.uploadCount }?.minUploads ?: 0
        val range = nextLevel.minUploads - prev
        ((teacher.uploadCount - prev).toFloat() / range).coerceIn(0f, 1f)
    } else 1f
    val animP by animateFloatAsState(progressToNext, tween(800), label = "lv")

    GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = levelColor.copy(0.4f)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Current Level", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(teacher.analysisLevel.label, style = MaterialTheme.typography.titleMedium,
                        color = levelColor, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Uploads", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text("${teacher.uploadCount}", style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
            if (nextLevel != null) {
                Text("${nextLevel.minUploads - teacher.uploadCount} more uploads to unlock ${nextLevel.label}",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Box(
                    modifier = Modifier.fillMaxWidth().height(10.dp)
                        .clip(RoundedCornerShape(5.dp)).background(CardDarker)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(animP).fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(Brush.horizontalGradient(listOf(levelColor.copy(0.5f), levelColor)))
                    )
                }
            } else {
                Text("🏆 Maximum analysis level reached!", style = MaterialTheme.typography.bodySmall,
                    color = AccentGold, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PerformanceSummaryCard(report: TeacherDetailReport) {
    val levelColor = Color(report.analysisLevel.color)
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Overall score ring
            Row(verticalAlignment = Alignment.CenterVertically) {
                val animScore by animateFloatAsState(report.avgOverallScore / 100f, tween(1000), label = "sc")
                CircularProgressIndicator(
                    progress = { animScore },
                    color = levelColor,
                    trackColor = levelColor.copy(0.1f),
                    strokeWidth = 8.dp,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("%.1f%%".format(report.avgOverallScore),
                        style = MaterialTheme.typography.displaySmall,
                        color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("Average Score", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (tIcon, tColor) = when (report.trend) {
                            "IMPROVING" -> Icons.Default.TrendingUp to EmeraldGreen
                            "DECLINING" -> Icons.Default.TrendingDown to AccentRed
                            else        -> Icons.Default.TrendingFlat to AccentGold
                        }
                        Icon(tIcon, null, tint = tColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(report.trend, style = MaterialTheme.typography.labelSmall,
                            color = tColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
            HorizontalDivider(color = GlassBorder)
            // Sub scores
            ScoreRow("Engagement",  report.avgEngagement, NeonBlue)
            ScoreRow("Voice Clarity", report.avgClarity, EmeraldGreen)
            ScoreRow("Board Usage", report.avgBoardUsage, NeonPurple)
        }
    }
}

@Composable
private fun ScoreRow(label: String, score: Float, color: Color) {
    val anim by animateFloatAsState(score / 100f, tween(800), label = "sr")
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text("%.1f%%".format(score), style = MaterialTheme.typography.labelLarge, color = color)
        }
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(CardDarker)) {
            Box(modifier = Modifier.fillMaxWidth(anim).fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(listOf(color.copy(0.5f), color))))
        }
    }
}

@Composable
private fun RecommendationsCard(report: TeacherDetailReport) {
    GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = Color(report.analysisLevel.color).copy(0.3f)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("AI Recommendations · ${report.analysisLevel.label}",
                style = MaterialTheme.typography.labelMedium,
                color = Color(report.analysisLevel.color), fontWeight = FontWeight.Bold)
            report.recommendations.forEach { rec ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Lightbulb, null, tint = AccentGold,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(rec, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
private fun EmptyRecordingsCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.VideoLibrary, null, tint = TextDisabled, modifier = Modifier.size(48.dp))
            Text("No recordings yet", style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            Text("Tap Upload to add the first recording",
                style = MaterialTheme.typography.bodySmall, color = TextDisabled)
        }
    }
}

@Composable
private fun RecordingRow(rec: RecordingEntry) {
    val levelColor = Color(rec.analysisLevel.color)
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val mins = rec.durationSeconds / 60
    val size = when {
        rec.fileSizeBytes < 1024 * 1024       -> "${rec.fileSizeBytes / 1024} KB"
        else -> "%.1f MB".format(rec.fileSizeBytes / 1024.0 / 1024.0)
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(levelColor.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.OndemandVideo, null, tint = levelColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(rec.filename, style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${mins}m · $size · ${sdf.format(Date(rec.uploadedAt))}",
                    style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("%.1f%%".format(rec.score), style = MaterialTheme.typography.titleSmall,
                    color = levelColor, fontWeight = FontWeight.Bold)
                Text(rec.analysisLevel.label, style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary)
            }
        }
    }
}

// ─── URI → File helper ─────────────────────────────────────
private fun copyUriToCache(context: Context, uri: Uri): File? {
    return try {
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "video_${System.currentTimeMillis()}.mp4"
        val dest = File(context.cacheDir, "uploads/$name")
        dest.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { ins ->
            FileOutputStream(dest).use { out -> ins.copyTo(out) }
        }
        dest
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
private fun AddTeacherDialog(
    onConfirm: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name       by remember { mutableStateOf("") }
    var subject    by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GlassSurface,
        title = { Text("Add Teacher", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue, unfocusedBorderColor = GlassBorder,
                        focusedLabelColor  = NeonBlue, cursorColor = NeonBlue,
                        focusedTextColor   = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )
                OutlinedTextField(
                    value = subject, onValueChange = { subject = it },
                    label = { Text("Subject") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue, unfocusedBorderColor = GlassBorder,
                        focusedLabelColor  = NeonBlue, cursorColor = NeonBlue,
                        focusedTextColor   = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )
                OutlinedTextField(
                    value = department, onValueChange = { department = it },
                    label = { Text("Department") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonBlue, unfocusedBorderColor = GlassBorder,
                        focusedLabelColor  = NeonBlue, cursorColor = NeonBlue,
                        focusedTextColor   = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(NeonBlue, NeonPurple)))
                    .clickable(enabled = name.isNotBlank() && subject.isNotBlank() && department.isNotBlank()) {
                        onConfirm(name.trim(), subject.trim(), department.trim())
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Add Teacher", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
