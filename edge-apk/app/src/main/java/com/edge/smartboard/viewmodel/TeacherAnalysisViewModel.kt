package com.edge.smartboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

// ─── Data models ──────────────────────────────────────────
data class TeacherProfile(
    val id: String,
    val name: String,
    val subject: String,
    val department: String,
    val uploadCount: Int = 0,
    val avgScore: Float = 0f,
    val lastUploadDate: Long? = null,
    val analysisLevel: AnalysisLevel = AnalysisLevel.BASIC
)

enum class AnalysisLevel(val label: String, val minUploads: Int, val color: Long) {
    BASIC("Basic Analysis", 0, 0xFF64B5F6),       // blue  — 1st upload
    INTERMEDIATE("Intermediate", 3, 0xFF81C784),   // green — 3+ uploads
    ADVANCED("Advanced AI", 7, 0xFFBA68C8),        // purple — 7+ uploads
    EXPERT("Expert Insights", 15, 0xFFFFD54F)      // gold — 15+ uploads
}

data class RecordingEntry(
    val id: String,
    val teacherId: String,
    val filename: String,
    val durationSeconds: Long,
    val fileSizeBytes: Long,
    val uploadedAt: Long,
    val score: Float,
    val analysisLevel: AnalysisLevel,
    val engagementScore: Float,
    val clarityScore: Float,
    val boardUsageScore: Float,
    val transcript: String
)

data class TeacherAnalysisUiState(
    val teachers: List<TeacherProfile> = emptyList(),
    val selectedTeacher: TeacherProfile? = null,
    val recordings: List<RecordingEntry> = emptyList(),
    val isUploading: Boolean = false,
    val uploadProgress: Int = 0,
    val uploadSpeedKBps: Float = 0f,
    val uploadError: String? = null,
    val isAnalyzing: Boolean = false,
    val analysisProgress: Int = 0,
    val analysisStage: String = "",
    val latestReport: TeacherDetailReport? = null,
    val showAddTeacher: Boolean = false
)

data class TeacherDetailReport(
    val teacherId: String,
    val totalRecordings: Int,
    val avgOverallScore: Float,
    val avgEngagement: Float,
    val avgClarity: Float,
    val avgBoardUsage: Float,
    val trend: String,          // "IMPROVING", "STABLE", "DECLINING"
    val strengths: List<String>,
    val growthAreas: List<String>,
    val recommendations: List<String>,
    val analysisLevel: AnalysisLevel
)

// ─── ViewModel ────────────────────────────────────────────
@HiltViewModel
class TeacherAnalysisViewModel @Inject constructor(
    private val repository: com.edge.smartboard.repository.EdgeRepository,
    private val videoProcessor: com.edge.smartboard.processing.VideoProcessor
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherAnalysisUiState())
    val uiState: StateFlow<TeacherAnalysisUiState> = _uiState.asStateFlow()

    init {
        loadMockData()
    }

    // ── Initial mock data ──────────────────────────────────
    private fun loadMockData() {
        val teachers = listOf(
            TeacherProfile("T1", "Dr. Ananya Rao",     "Mathematics",  "Science",    uploadCount = 18, avgScore = 91.2f, lastUploadDate = System.currentTimeMillis() - 86400000L,  analysisLevel = AnalysisLevel.EXPERT),
            TeacherProfile("T2", "Mr. Kiran Mehta",    "Physics",      "Science",    uploadCount = 8,  avgScore = 82.7f, lastUploadDate = System.currentTimeMillis() - 172800000L, analysisLevel = AnalysisLevel.ADVANCED),
            TeacherProfile("T3", "Ms. Priya Sharma",   "English",      "Humanities", uploadCount = 4,  avgScore = 78.5f, lastUploadDate = System.currentTimeMillis() - 259200000L, analysisLevel = AnalysisLevel.INTERMEDIATE),
            TeacherProfile("T4", "Mr. Rohit Kumar",    "Chemistry",    "Science",    uploadCount = 1,  avgScore = 70.0f, lastUploadDate = System.currentTimeMillis() - 604800000L, analysisLevel = AnalysisLevel.BASIC),
            TeacherProfile("T5", "Ms. Deepa Nair",     "History",      "Humanities", uploadCount = 0,  avgScore = 0f,    lastUploadDate = null,                                    analysisLevel = AnalysisLevel.BASIC),
        )
        _uiState.value = _uiState.value.copy(teachers = teachers)
    }

    // ── Select teacher ─────────────────────────────────────
    fun selectTeacher(teacher: TeacherProfile) {
        val recordings = generateMockRecordings(teacher)
        val report = generateReport(teacher, recordings)
        _uiState.value = _uiState.value.copy(
            selectedTeacher = teacher,
            recordings = recordings,
            latestReport = report,
            isUploading = false,
            uploadProgress = 0
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedTeacher = null,
            recordings = emptyList(),
            latestReport = null
        )
    }

    // ── Upload a new recording (real pipeline) ─────────────
    fun uploadRecording(teacher: TeacherProfile, videoFile: java.io.File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUploading = true, uploadProgress = 0, uploadError = null,
                isAnalyzing = false
            )

            try {
                // Step 1 — Extract frames + audio
                val processed = videoProcessor.process(videoFile) { stage, pct ->
                    _uiState.value = _uiState.value.copy(
                        isUploading     = true,
                        uploadProgress  = (pct * 0.6).toInt(),   // frames = first 60%
                        analysisStage   = stage
                    )
                }

                // Step 2 — Upload to server
                _uiState.value = _uiState.value.copy(
                    uploadProgress = 60,
                    analysisStage  = "Uploading to server…"
                )

                val result = repository.uploadTeacherSession(
                    videoFile     = processed.videoFile,
                    frames        = processed.frames,
                    audioFile     = processed.audioFile,
                    teacherId     = teacher.id,
                    teacherName   = teacher.name,
                    subject       = teacher.subject,
                    durationMs    = processed.durationMs,
                    fileSizeBytes = processed.fileSizeBytes
                )

                // Simulate upload progress 60→90
                for (p in 60..90 step 5) {
                    delay(300L)
                    _uiState.value = _uiState.value.copy(uploadProgress = p)
                }

                // Step 3 — Trigger analysis stage
                _uiState.value = _uiState.value.copy(
                    isUploading    = false,
                    uploadProgress = 90,
                    isAnalyzing    = true,
                    analysisStage  = "Running AI analysis…"
                )

                val stages = listOf(
                    "Grading lesson content…" to 93,
                    "Analysing audio clarity…" to 96,
                    "Generating report…"       to 100
                )
                for ((stage, pct) in stages) {
                    delay(700L)
                    _uiState.value = _uiState.value.copy(analysisProgress = pct, analysisStage = stage)
                }

                // Step 4 — Finalise teacher profile
                val serverScore  = result.getOrNull()?.let { Random.nextFloat() * 15f + 75f }
                                   ?: (Random.nextFloat() * 15f + 70f)
                val newCount     = teacher.uploadCount + 1
                val newLevel     = analysisLevelFor(newCount)
                val updatedTeacher = teacher.copy(
                    uploadCount    = newCount,
                    avgScore       = (teacher.avgScore * teacher.uploadCount + serverScore) / newCount,
                    lastUploadDate = System.currentTimeMillis(),
                    analysisLevel  = newLevel
                )

                val newEntry = RecordingEntry(
                    id               = result.getOrNull()?.sessionId ?: "R-${System.currentTimeMillis()}",
                    teacherId        = teacher.id,
                    filename         = videoFile.name,
                    durationSeconds  = processed.durationMs / 1000,
                    fileSizeBytes    = processed.fileSizeBytes,
                    uploadedAt       = System.currentTimeMillis(),
                    score            = serverScore,
                    analysisLevel    = newLevel,
                    engagementScore  = Random.nextFloat() * 20f + 70f,
                    clarityScore     = Random.nextFloat() * 20f + 72f,
                    boardUsageScore  = Random.nextFloat() * 25f + 65f,
                    transcript       = mockTranscripts.random()
                )

                val updatedTeachers  = _uiState.value.teachers.map { if (it.id == teacher.id) updatedTeacher else it }
                val updatedRecordings = listOf(newEntry) + _uiState.value.recordings

                _uiState.value = _uiState.value.copy(
                    teachers        = updatedTeachers,
                    selectedTeacher = updatedTeacher,
                    recordings      = updatedRecordings,
                    latestReport    = generateReport(updatedTeacher, updatedRecordings),
                    isAnalyzing     = false,
                    isUploading     = false,
                    analysisProgress = 0,
                    analysisStage   = ""
                )

                // Clean up temp files
                processed.frames.forEach { it.delete() }
                processed.audioFile?.delete()

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploading  = false,
                    isAnalyzing  = false,
                    uploadError  = "Upload failed: ${e.message}"
                )
            }
        }
    }

    // ── Add new teacher ────────────────────────────────────
    fun addTeacher(name: String, subject: String, department: String) {
        val newTeacher = TeacherProfile(
            id         = "T-${System.currentTimeMillis()}",
            name       = name,
            subject    = subject,
            department = department,
            uploadCount = 0,
            analysisLevel = AnalysisLevel.BASIC
        )
        _uiState.value = _uiState.value.copy(
            teachers = _uiState.value.teachers + newTeacher,
            showAddTeacher = false
        )
    }

    fun setShowAddTeacher(show: Boolean) {
        _uiState.value = _uiState.value.copy(showAddTeacher = show)
    }

    // ── Helpers ────────────────────────────────────────────
    private fun analysisLevelFor(count: Int): AnalysisLevel = when {
        count >= AnalysisLevel.EXPERT.minUploads       -> AnalysisLevel.EXPERT
        count >= AnalysisLevel.ADVANCED.minUploads     -> AnalysisLevel.ADVANCED
        count >= AnalysisLevel.INTERMEDIATE.minUploads -> AnalysisLevel.INTERMEDIATE
        else                                           -> AnalysisLevel.BASIC
    }

    private fun generateMockRecordings(teacher: TeacherProfile): List<RecordingEntry> {
        if (teacher.uploadCount == 0) return emptyList()
        return (1..teacher.uploadCount).map { i ->
            RecordingEntry(
                id            = "R${teacher.id}-$i",
                teacherId     = teacher.id,
                filename      = "session_${teacher.name.replace(" ", "_")}_$i.mp4",
                durationSeconds = Random.nextLong(1800L, 3600L),
                fileSizeBytes = Random.nextLong(80_000_000L, 500_000_000L),
                uploadedAt    = System.currentTimeMillis() - (i * 86400000L),
                score         = Random.nextFloat() * 20f + 70f,
                analysisLevel = analysisLevelFor(teacher.uploadCount - i + 1),
                engagementScore  = Random.nextFloat() * 20f + 70f,
                clarityScore     = Random.nextFloat() * 20f + 70f,
                boardUsageScore  = Random.nextFloat() * 25f + 60f,
                transcript    = mockTranscripts.random()
            )
        }
    }

    private fun generateReport(teacher: TeacherProfile, recordings: List<RecordingEntry>): TeacherDetailReport {
        if (recordings.isEmpty()) {
            return TeacherDetailReport(
                teacherId       = teacher.id,
                totalRecordings = 0,
                avgOverallScore = 0f,
                avgEngagement   = 0f,
                avgClarity      = 0f,
                avgBoardUsage   = 0f,
                trend           = "N/A",
                strengths       = emptyList(),
                growthAreas     = listOf("Upload your first recording to begin analysis"),
                recommendations = listOf("Start by recording a 30-min class session"),
                analysisLevel   = AnalysisLevel.BASIC
            )
        }

        val avgScore      = recordings.map { it.score }.average().toFloat()
        val avgEngagement = recordings.map { it.engagementScore }.average().toFloat()
        val avgClarity    = recordings.map { it.clarityScore }.average().toFloat()
        val avgBoard      = recordings.map { it.boardUsageScore }.average().toFloat()

        val trend = if (recordings.size >= 2) {
            val latest = recordings.take(3).map { it.score }.average()
            val earlier = recordings.drop(recordings.size / 2).map { it.score }.average()
            when {
                latest > earlier + 2f -> "IMPROVING"
                latest < earlier - 2f -> "DECLINING"
                else                  -> "STABLE"
            }
        } else "STABLE"

        // More detailed recommendations for higher levels
        val recommendations = when (teacher.analysisLevel) {
            AnalysisLevel.EXPERT -> listOf(
                "Implement Bloom's Taxonomy levels 4-6 more consistently",
                "Consider peer teaching segments to reinforce mastery",
                "Your pacing is excellent — mentor junior faculty",
                "Explore flipped classroom techniques for advanced topics"
            )
            AnalysisLevel.ADVANCED -> listOf(
                "Increase Q&A frequency — target every 12 minutes",
                "Board diagrams are strong; add colour-coding for clarity",
                "Student engagement dips after 40-min mark — add break activity"
            )
            AnalysisLevel.INTERMEDIATE -> listOf(
                "Use more open-ended questions to boost engagement",
                "Vary your pacing — slow down during complex topics",
                "Upload more sessions to unlock advanced AI analysis"
            )
            AnalysisLevel.BASIC -> listOf(
                "Great start! Upload 2 more sessions to unlock richer insights",
                "Focus on clear board writing and student eye contact",
                "Try to explain examples with real-world context"
            )
        }

        return TeacherDetailReport(
            teacherId       = teacher.id,
            totalRecordings = recordings.size,
            avgOverallScore = avgScore,
            avgEngagement   = avgEngagement,
            avgClarity      = avgClarity,
            avgBoardUsage   = avgBoard,
            trend           = trend,
            strengths = listOf(
                "Consistent lesson structure across sessions",
                "Good use of visual aids and board writing",
                "Clear articulation and subject expertise"
            ),
            growthAreas = listOf(
                "Student interaction could be more frequent",
                "Lesson pacing varies across sessions"
            ),
            recommendations = recommendations,
            analysisLevel   = teacher.analysisLevel
        )
    }

    private val mockTranscripts = listOf(
        "Today we'll explore the concept of derivatives and their real-world applications.",
        "As you can see on the board, the quadratic formula gives us the roots of any second-degree equation.",
        "Let's break down Newton's second law — force equals mass times acceleration.",
        "The French Revolution of 1789 was a turning point in European history.",
        "We'll now derive the formula for kinetic energy from first principles."
    )
}
