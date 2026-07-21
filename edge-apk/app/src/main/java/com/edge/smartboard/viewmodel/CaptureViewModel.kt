package com.edge.smartboard.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edge.smartboard.models.RecordingState
import com.edge.smartboard.repository.EdgeRepository
import com.edge.smartboard.ui.capture.ScreenRecordingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val repository: EdgeRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _recordingState = MutableStateFlow(RecordingState())
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private var pollingJob: Job? = null
    private var projectionData: Intent? = null
    private var projectionResultCode: Int = 0

    companion object {
        private const val TAG = "CaptureVM"
    }

    // ── Projection grant ─────────────────────────────────────────
    fun onProjectionGranted(resultCode: Int, data: Intent) {
        projectionResultCode = resultCode
        projectionData = data
    }

    // ── Start recording ──────────────────────────────────────────
    fun startRecording() {
        val data = projectionData ?: run {
            Log.e(TAG, "startRecording() called without projection data")
            return
        }
        val intent = Intent(appContext, ScreenRecordingService::class.java).apply {
            putExtra("resultCode", projectionResultCode)
            putExtra("data", data)
        }
        appContext.startForegroundService(intent)
        _recordingState.value = RecordingState(isRecording = true, isPaused = false)
        startPolling()
    }

    // ── Stop recording ───────────────────────────────────────────
    fun stopRecording() {
        Log.d(TAG, "stopRecording() called")
        pollingJob?.cancel()

        // Send ACTION_STOP so the service stops gracefully (calls stopRecordingAndSelf)
        val stopIntent = Intent(appContext, ScreenRecordingService::class.java).apply {
            action = ScreenRecordingService.ACTION_STOP
        }
        appContext.startService(stopIntent)

        // Give the service a short window to finish writing the file, then read state
        viewModelScope.launch {
            // Wait up to 2 seconds for the service to stop
            var waited = 0
            while (ScreenRecordingService.isRunning && waited < 2000) {
                delay(100L)
                waited += 100
            }

            val savedPath     = ScreenRecordingService.savedFilePath
            val duration      = ScreenRecordingService.durationSeconds
            val size          = ScreenRecordingService.fileSizeBytes

            Log.d(TAG, "After stop: path=$savedPath, duration=$duration, size=$size")

            _recordingState.value = RecordingState(
                isRecording     = false,
                isPaused        = false,
                savedFilePath   = savedPath,
                durationSeconds = duration,
                fileSizeBytes   = size
            )
        }
    }

    // ── Poll service for live state ──────────────────────────────
    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(500L)
                if (!ScreenRecordingService.isRunning) {
                    // Service stopped externally (e.g. notification button)
                    Log.d(TAG, "Service stopped externally — reading final state")
                    delay(300L) // brief wait for file flush
                    _recordingState.value = _recordingState.value.copy(
                        isRecording     = false,
                        isPaused        = false,
                        savedFilePath   = ScreenRecordingService.savedFilePath,
                        durationSeconds = ScreenRecordingService.durationSeconds,
                        fileSizeBytes   = ScreenRecordingService.fileSizeBytes
                    )
                    break
                }
                _recordingState.value = _recordingState.value.copy(
                    durationSeconds  = ScreenRecordingService.durationSeconds,
                    fileSizeBytes    = ScreenRecordingService.fileSizeBytes
                )
            }
        }
    }

    // ── Reset after viewing saved file ───────────────────────────
    fun resetSavedFile() {
        _recordingState.value = RecordingState()
        ScreenRecordingService.savedFilePath    = null
        ScreenRecordingService.durationSeconds  = 0L
        ScreenRecordingService.fileSizeBytes    = 0L
        // Reset the projection data so user must re-grant on next start
        projectionData       = null
        projectionResultCode = 0
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
