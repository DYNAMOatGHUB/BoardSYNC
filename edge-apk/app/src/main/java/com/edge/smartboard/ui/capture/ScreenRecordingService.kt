package com.edge.smartboard.ui.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Foreground Service — records the screen as MP4 using MediaProjection + MediaRecorder.
 *
 * Android 14 (API 34) FIX:
 *   startForeground() with FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION MUST be called
 *   BEFORE getMediaProjection(). The system links the two calls by the notification.
 */
class ScreenRecordingService : Service() {

    companion object {
        const val CHANNEL_ID  = "screen_recording_channel"
        const val NOTIF_ID    = 2001
        const val ACTION_STOP = "com.edge.smartboard.STOP_RECORDING"
        private const val TAG = "ScreenRecSvc"

        @Volatile var isRunning: Boolean  = false
        @Volatile var savedFilePath: String? = null
        @Volatile var durationSeconds: Long  = 0L
        @Volatile var fileSizeBytes: Long    = 0L
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay:  VirtualDisplay?  = null
    private var mediaRecorder:   MediaRecorder?   = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var startTimeMs = 0L
    private var outputFile: File? = null
    private var isStopping = false

    private var screenWidth  = 1080
    private var screenHeight = 1920
    private var screenDpi    = 320

    // ── Lifecycle ───────────────────────────────────────────────

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopRecordingAndSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra("resultCode", 0) ?: 0
        @Suppress("DEPRECATION")
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("data", Intent::class.java)
        } else {
            intent?.getParcelableExtra("data")
        }

        if (resultCode == 0 || data == null) {
            Log.e(TAG, "Missing resultCode or data — cannot start")
            stopSelf()
            return START_NOT_STICKY
        }

        // ── STEP 1: Create notification channel & call startForeground FIRST ──
        // Android 14 requires this to happen BEFORE getMediaProjection().
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                buildNotification("Starting recording…"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIF_ID, buildNotification("Starting recording…"))
        }

        // ── STEP 2: Get screen metrics ────────────────────────────────────────
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        screenWidth  = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDpi    = metrics.densityDpi

        // ── STEP 3: Obtain MediaProjection AFTER startForeground ──────────────
        val projManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        try {
            mediaProjection = projManager.getMediaProjection(resultCode, data)
        } catch (e: Exception) {
            Log.e(TAG, "getMediaProjection failed: ${e.message}", e)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.d(TAG, "MediaProjection stopped by system")
                stopRecordingAndSelf()
            }
        }, null)

        // ── STEP 4: Start recording ───────────────────────────────────────────
        startRecording()
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        if (!isStopping) stopRecording()
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Recording ────────────────────────────────────────────────

    private fun startRecording() {
        try {
            val dir = File(getExternalFilesDir(null), "recordings").also { it.mkdirs() }
            val ts  = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            outputFile = File(dir, "recording_$ts.mp4")

            val recW = (screenWidth  / 2).coerceAtLeast(720)
            val recH = (screenHeight / 2).coerceAtLeast(1280)

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder!!.apply {
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setVideoSize(recW, recH)
                setVideoFrameRate(30)
                setVideoEncodingBitRate(5_000_000)
                setOutputFile(outputFile!!.absolutePath)
                prepare()
            }

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "EdgeRecording",
                recW, recH, screenDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder!!.surface, null, null
            )

            mediaRecorder!!.start()

            // Set state only after successful start
            savedFilePath   = outputFile!!.absolutePath
            isRunning       = true
            startTimeMs     = System.currentTimeMillis()
            durationSeconds = 0L
            fileSizeBytes   = 0L

            Log.d(TAG, "Recording started → ${outputFile!!.absolutePath}")

            // Live timer update
            serviceScope.launch {
                while (isActive && isRunning) {
                    delay(1000L)
                    durationSeconds = (System.currentTimeMillis() - startTimeMs) / 1000L
                    fileSizeBytes   = outputFile?.length() ?: 0L
                    updateNotification(
                        "● REC  ${formatDuration(durationSeconds)}  ${formatBytes(fileSizeBytes)}"
                    )
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "startRecording failed: ${e.message}", e)
            savedFilePath = null
            isRunning     = false
            try { mediaRecorder?.release() } catch (_: Exception) {}
            mediaRecorder = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopRecording() {
        try { mediaRecorder?.stop()    } catch (e: Exception) { Log.w(TAG, "stop: ${e.message}") }
        try { mediaRecorder?.release() } catch (_: Exception) {}
        try { virtualDisplay?.release() } catch (_: Exception) {}
        try { mediaProjection?.stop()   } catch (_: Exception) {}

        mediaRecorder  = null
        virtualDisplay = null
        mediaProjection = null

        fileSizeBytes = outputFile?.let { if (it.exists()) it.length() else 0L } ?: 0L
        Log.d(TAG, "Stopped. File=${savedFilePath} size=${fileSizeBytes}")
    }

    private fun stopRecordingAndSelf() {
        if (isStopping) return
        isStopping = true
        isRunning  = false
        stopRecording()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Notification ─────────────────────────────────────────────

    private fun buildNotification(text: String): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, ScreenRecordingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Edge Smartboard — Recording")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_pause, "Stop & Save", stopIntent)
            .build()
    }

    private fun updateNotification(text: String) =
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(text))

    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "Screen Recording", NotificationManager.IMPORTANCE_LOW)
            .apply { description = "Screen recording foreground service" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun formatDuration(s: Long) = "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
    private fun formatBytes(b: Long) = when {
        b <= 0      -> "0 B"
        b < 1024    -> "$b B"
        b < 1048576 -> "%.1f KB".format(b / 1024.0)
        else        -> "%.1f MB".format(b / 1048576.0)
    }
}
