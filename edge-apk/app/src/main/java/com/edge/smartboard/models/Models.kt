package com.edge.smartboard.models

import androidx.room.Entity
import androidx.room.PrimaryKey

// ─── Auth ────────────────────────────────────────────────
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val user: UserInfo
)

data class UserInfo(
    val id: String,
    val name: String,
    val email: String,
    val school: String,
    val role: String,
    val avatar: String? = null
)

// ─── Session ─────────────────────────────────────────────
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey val sessionId: String,
    val teacherName: String,
    val subject: String,
    val classRoom: String,
    val department: String,
    val date: Long,
    val durationSeconds: Long = 0L,
    val imageCount: Int = 0,
    val audioFile: String? = null,
    val storageBytes: Long = 0L,
    val status: SessionStatus = SessionStatus.RECORDING,
    val score: Float? = null
)

enum class SessionStatus { RECORDING, COMPLETED, UPLOADING, PROCESSING, FAILED, UPLOADED }

// ─── Screen Recording ────────────────────────────────────
data class RecordingState(
    val isRecording: Boolean     = false,
    val isPaused: Boolean        = false,
    val durationSeconds: Long    = 0L,
    val fileSizeBytes: Long      = 0L,
    val savedFilePath: String?   = null
)

// ─── Capture (legacy, kept for backward compat) ──────────
data class CaptureState(
    val isCapturing: Boolean     = false,
    val isPaused: Boolean        = false,
    val currentFps: Int          = 1,
    val frameCount: Int          = 0,
    val uniqueFrames: Int        = 0,
    val duplicatesRemoved: Int   = 0,
    val compressionRatio: Float  = 0f,
    val quality: CaptureQuality  = CaptureQuality.MEDIUM
)

enum class CaptureQuality { LOW, MEDIUM, HIGH }

// ─── Audio ───────────────────────────────────────────────
data class AudioState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val durationMs: Long = 0L,
    val audioLevels: List<Float> = emptyList(),
    val savedFilePath: String? = null
)

// ─── Upload ──────────────────────────────────────────────
data class UploadState(
    val isUploading: Boolean = false,
    val progressPercent: Int = 0,
    val speedKBps: Float = 0f,
    val remainingSeconds: Long = 0L,
    val sessionId: String? = null
)

// ─── AI Status ───────────────────────────────────────────
data class QueueStatus(
    val name: String,
    val length: Int,
    val status: PipelineStatus,
    val latencyMs: Long = 0L
)

enum class PipelineStatus { IDLE, PROCESSING, COMPLETED, FAILED }

data class SystemMetrics(
    val cpuPercent: Float = 0f,
    val gpuMemoryMb: Int = 0,
    val gpuPercent: Float = 0f,
    val queues: List<QueueStatus> = emptyList()
)

// ─── Report ──────────────────────────────────────────────
data class Report(
    val sessionId: String,
    val overallScore: Float,
    val lessonCoverage: Float,
    val studentEngagement: Float,
    val speakingTime: Float,
    val boardUsage: Float,
    val visualTeachingScore: Float,
    val voiceClarity: Float,
    val curriculumAlignment: Float,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val recommendations: List<String>
)

// ─── Live Processing ─────────────────────────────────────
data class LiveFrame(
    val frameIndex: Int,
    val transcript: String,
    val speakerLabel: String,
    val detectedText: String,
    val visualContext: String,
    val diagramDetected: Boolean,
    val mathDetected: Boolean,
    val chartDetected: Boolean,
    val whiteboardDetected: Boolean,
    val currentAgent: AgentType
)

enum class AgentType {
    RETRIEVER, CONTENT_GRADER, PEDAGOGY, AUDITOR, REPORT_WRITER
}

// ─── Dashboard ───────────────────────────────────────────
data class DashboardState(
    val screenCaptureActive: Boolean = false,
    val audioRecordingActive: Boolean = false,
    val aiProcessingActive: Boolean = false,
    val uploadQueueSize: Int = 0,
    val gpuStatus: Float = 0f,
    val networkHealthMs: Long = 0L,
    val sessionDurationSeconds: Long = 0L,
    val deviceName: String = "Edge Smartboard",
    val schoolName: String = "Loading...",
    val notifications: Int = 0
)

// ─── Notification ────────────────────────────────────────
data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false
)

enum class NotificationType {
    LOW_CONFIDENCE, UPLOAD_FAILED, QUEUE_FULL, AI_FINISHED, REPORT_READY, NETWORK_LOST
}

// ─── Admin Dashboard ─────────────────────────────────────
data class AdminDashboard(
    val totalSessions: Int = 0,
    val todaySessions: Int = 0,
    val avgTeachingScore: Float = 0f,
    val gpuUtilization: Float = 0f,
    val cpuUtilization: Float = 0f,
    val storageUsedGb: Float = 0f,
    val uploadSuccessRate: Float = 0f,
    val weeklyData: List<Float> = emptyList(),
    val monthlyData: List<Float> = emptyList(),
    val departmentData: Map<String, Float> = emptyMap()
)

// ─── History item ────────────────────────────────────────
data class HistoryItem(
    val session: Session,
    val report: Report? = null
)

// ─── API responses ───────────────────────────────────────
data class UploadResponse(
    val success: Boolean,
    val sessionId: String,
    val message: String
)

data class ApiError(
    val detail: String
)

// ─── Teacher upload ───────────────────────────────────────
data class TeacherUploadResponse(
    val success: Boolean,
    val sessionId: String,
    val teacherId: String,
    val framesReceived: Int,
    val audioReceived: Boolean,
    val message: String
)

// ─── Local Recording (Room entity) ───────────────────────
@Entity(tableName = "local_recordings")
data class LocalRecording(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // File paths
    val videoPath: String,
    val framesDir: String?,          // dir containing extracted JPEGs
    val audioPath: String?,          // extracted WAV file

    // Metadata
    val filename: String,
    val durationSeconds: Long,
    val fileSizeBytes: Long,
    val frameCount: Int,
    val recordedAt: Long = System.currentTimeMillis(),

    // Upload / processing
    val uploadStatus: String = "PENDING",  // PENDING | PROCESSING | UPLOADED | FAILED
    val serverSessionId: String? = null,
    val analysisScore: Float? = null,
    val errorMessage: String? = null
)
