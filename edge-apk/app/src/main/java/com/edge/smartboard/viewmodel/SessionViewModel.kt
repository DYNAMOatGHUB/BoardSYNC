package com.edge.smartboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edge.smartboard.models.*
import com.edge.smartboard.repository.EdgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repository: EdgeRepository
) : ViewModel() {

    val sessions = repository.getAllSessions()

    private val _uploadState = MutableStateFlow(UploadState())
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    private val _selectedSession = MutableStateFlow<Session?>(null)
    val selectedSession: StateFlow<Session?> = _selectedSession.asStateFlow()

    private val _report = MutableStateFlow<Report?>(null)
    val report: StateFlow<Report?> = _report.asStateFlow()

    private val _liveFrame = MutableStateFlow<LiveFrame?>(null)
    val liveFrame: StateFlow<LiveFrame?> = _liveFrame.asStateFlow()

    fun createSession(
        teacherName: String,
        subject: String,
        classRoom: String,
        department: String
    ): Session {
        val session = Session(
            sessionId    = "SID-${System.currentTimeMillis()}",
            teacherName  = teacherName,
            subject      = subject,
            classRoom    = classRoom,
            department   = department,
            date         = System.currentTimeMillis(),
            status       = SessionStatus.RECORDING
        )
        viewModelScope.launch { repository.saveSession(session) }
        return session
    }

    fun selectSession(session: Session) {
        _selectedSession.value = session
    }

    fun startUpload(session: Session) {
        viewModelScope.launch {
            _uploadState.value = UploadState(isUploading = true, sessionId = session.sessionId)
            // Simulate upload progress
            for (i in 1..100) {
                delay(80L)
                val speed = Random.nextFloat() * 500 + 200
                val remaining = ((100 - i) / 100f * 30).toLong()
                _uploadState.value = _uploadState.value.copy(
                    progressPercent  = i,
                    speedKBps        = speed,
                    remainingSeconds = remaining
                )
            }
            _uploadState.value = UploadState(isUploading = false, progressPercent = 100)
            // Update session status
            val updated = session.copy(status = SessionStatus.UPLOADED)
            repository.updateSession(updated)
        }
    }

    fun cancelUpload() {
        _uploadState.value = UploadState()
    }

    fun retryUpload(session: Session) = startUpload(session)

    fun loadReport(sessionId: String) {
        viewModelScope.launch {
            val result = repository.getReport(sessionId)
            _report.value = result.getOrNull() ?: generateMockReport(sessionId)
        }
    }

    fun startLiveProcessing(sessionId: String) {
        viewModelScope.launch {
            val agents = AgentType.values()
            var frameIdx = 0
            while (true) {
                delay(1500L)
                _liveFrame.value = LiveFrame(
                    frameIndex        = frameIdx++,
                    transcript        = mockTranscripts.random(),
                    speakerLabel      = "Teacher",
                    detectedText      = "Board text: ${mockBoardTexts.random()}",
                    visualContext     = "Classroom lecture mode",
                    diagramDetected   = Random.nextBoolean(),
                    mathDetected      = Random.nextBoolean(),
                    chartDetected     = Random.nextBoolean(),
                    whiteboardDetected = true,
                    currentAgent      = agents.random()
                )
            }
        }
    }

    private fun generateMockReport(sessionId: String) = Report(
        sessionId            = sessionId,
        overallScore         = 84.5f,
        lessonCoverage       = 91.2f,
        studentEngagement    = 78.6f,
        speakingTime         = 82.3f,
        boardUsage           = 88.1f,
        visualTeachingScore  = 86.4f,
        voiceClarity         = 90.2f,
        curriculumAlignment  = 87.7f,
        strengths            = listOf(
            "Excellent board utilization with visual diagrams",
            "Clear and structured lesson delivery",
            "Strong student interaction segments"
        ),
        weaknesses           = listOf(
            "Some segments lacked student interaction",
            "Pacing could be improved in sections 3-4"
        ),
        recommendations      = listOf(
            "Increase Q&A intervals every 15 minutes",
            "Use more visual aids for complex topics",
            "Consider adding collaborative activities"
        )
    )

    private val mockTranscripts = listOf(
        "Today we will explore the fundamentals of photosynthesis.",
        "As we can see on the board, the equation shows us...",
        "Can anyone tell me what ATP stands for?",
        "Let me draw this diagram to clarify the process.",
        "The key takeaway from this section is the energy transfer."
    )

    private val mockBoardTexts = listOf(
        "ATP = Adenosine Triphosphate",
        "6CO₂ + 6H₂O → C₆H₁₂O₆ + 6O₂",
        "Chloroplast → Thylakoid → Stroma",
        "Light Reaction + Calvin Cycle"
    )
}
