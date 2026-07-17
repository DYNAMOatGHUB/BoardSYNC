package com.edge.smartboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edge.smartboard.models.*
import com.edge.smartboard.repository.EdgeRepository
import com.edge.smartboard.ui.capture.ScreenRecordingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: EdgeRepository
) : ViewModel() {

    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    private val _systemMetrics = MutableStateFlow(SystemMetrics())
    val systemMetrics: StateFlow<SystemMetrics> = _systemMetrics.asStateFlow()

    private val _adminDashboard = MutableStateFlow(AdminDashboard())
    val adminDashboard: StateFlow<AdminDashboard> = _adminDashboard.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    init {
        startPolling()
        loadMockNotifications()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                refreshDashboard()
                delay(5000L) // Poll every 5s
            }
        }
    }

    private fun refreshDashboard() {
        viewModelScope.launch {
            // Try real API first, fall back to mock data
            val statusResult = repository.getSystemStatus()
            if (statusResult.isSuccess) {
                _systemMetrics.value = statusResult.getOrNull() ?: generateMockMetrics()
            } else {
                _systemMetrics.value = generateMockMetrics()
            }

            val adminResult = repository.getAdminDashboard()
            if (adminResult.isSuccess) {
                _adminDashboard.value = adminResult.getOrNull() ?: generateMockAdmin()
            } else {
                _adminDashboard.value = generateMockAdmin()
            }

            // Read real screen recording status from the service singleton
            _dashboardState.value = _dashboardState.value.copy(
                screenCaptureActive = ScreenRecordingService.isRunning,
                networkHealthMs     = Random.nextLong(10, 80),
                gpuStatus           = Random.nextFloat() * 0.8f
            )
        }
    }

    fun updateCaptureStatus(active: Boolean) {
        _dashboardState.value = _dashboardState.value.copy(screenCaptureActive = active)
    }

    fun updateAudioStatus(active: Boolean) {
        _dashboardState.value = _dashboardState.value.copy(audioRecordingActive = active)
    }

    fun updateSessionDuration(seconds: Long) {
        _dashboardState.value = _dashboardState.value.copy(sessionDurationSeconds = seconds)
    }

    fun addNotification(notification: AppNotification) {
        _notifications.value = listOf(notification) + _notifications.value
        _dashboardState.value = _dashboardState.value.copy(
            notifications = _notifications.value.count { !it.isRead }
        )
    }

    fun markAllRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
        _dashboardState.value = _dashboardState.value.copy(notifications = 0)
    }

    private fun loadMockNotifications() {
        _notifications.value = listOf(
            AppNotification("1", NotificationType.AI_FINISHED, "AI Processing Complete",
                "Session SID-001 analysis done. Score: 87%", System.currentTimeMillis() - 300000),
            AppNotification("2", NotificationType.REPORT_READY, "Report Ready",
                "Teaching report for Dr. Smith is ready to download.", System.currentTimeMillis() - 600000),
            AppNotification("3", NotificationType.UPLOAD_FAILED, "Upload Failed",
                "Session SID-002 upload failed. Tap to retry.", System.currentTimeMillis() - 900000, isRead = true),
        )
        _dashboardState.value = _dashboardState.value.copy(notifications = 2)
    }

    private fun generateMockMetrics() = SystemMetrics(
        cpuPercent   = Random.nextFloat() * 60 + 10,
        gpuPercent   = Random.nextFloat() * 70 + 5,
        gpuMemoryMb  = Random.nextInt(2000, 6000),
        queues = listOf(
            QueueStatus("CPU Queue",     Random.nextInt(0, 8),  PipelineStatus.PROCESSING),
            QueueStatus("GPU Queue",     Random.nextInt(0, 5),  PipelineStatus.IDLE),
            QueueStatus("Vision Queue",  Random.nextInt(0, 12), PipelineStatus.PROCESSING),
            QueueStatus("Audio Queue",   Random.nextInt(0, 6),  PipelineStatus.COMPLETED),
            QueueStatus("Reasoning Queue", Random.nextInt(0, 4), PipelineStatus.IDLE),
        )
    )

    private fun generateMockAdmin() = AdminDashboard(
        totalSessions     = 247,
        todaySessions     = 12,
        avgTeachingScore  = 84.3f,
        gpuUtilization    = Random.nextFloat() * 80 + 10,
        cpuUtilization    = Random.nextFloat() * 60 + 10,
        storageUsedGb     = 128.4f,
        uploadSuccessRate = 97.2f,
        weeklyData        = listOf(72f, 85f, 78f, 91f, 88f, 76f, 84f),
        monthlyData       = listOf(80f, 83f, 81f, 87f),
        departmentData    = mapOf("Science" to 88f, "Math" to 82f, "Eng" to 85f, "History" to 79f)
    )
}
