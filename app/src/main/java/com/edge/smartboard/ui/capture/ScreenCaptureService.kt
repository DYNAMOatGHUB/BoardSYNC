package com.edge.smartboard.ui.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.app.Service
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

/**
 * Foreground service for MediaProjection-based JPEG frame capture.
 * Used for session frame capture (upload to AI backend).
 *
 * Fixed for Android 14+: MediaProjection is created before startForeground().
 */
class ScreenCaptureService : Service() {

    companion object {
        const val CHANNEL_ID  = "screen_capture_channel"
        const val NOTIF_ID    = 1001
        const val ACTION_STOP = "com.edge.smartboard.STOP_CAPTURE"
        private const val TAG = "ScreenCapSvc"

        // Shared output dir so other components can read saved frames
        @Volatile var outputDir: File? = null
        @Volatile var frameCount: Int  = 0
        @Volatile var isRunning: Boolean = false
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay?   = null
    private var imageReader: ImageReader?         = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var screenWidth  = 1080
    private var screenHeight = 1920
    private var screenDpi    = 320

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val resultCode = intent?.getIntExtra("resultCode", 0) ?: 0
        @Suppress("DEPRECATION")
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("data", Intent::class.java)
        } else {
            intent?.getParcelableExtra<Intent>("data")
        }
        val fps = intent?.getIntExtra("fps", 1) ?: 1

        if (resultCode == 0 || data == null) {
            Log.e(TAG, "Missing resultCode or data")
            stopSelf()
            return START_NOT_STICKY
        }

        // Get real screen metrics
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        screenWidth  = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDpi    = metrics.densityDpi

        // Create MediaProjection BEFORE startForeground (required on API 34+)
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { stopSelf() }
        }, null)

        // Now start foreground
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                buildNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }

        // Create output directory
        val dir = File(getExternalFilesDir(null), "capture_${System.currentTimeMillis()}")
        dir.mkdirs()
        outputDir  = dir
        frameCount = 0
        isRunning  = true

        // Set up ImageReader for frame capture
        val captureWidth  = (screenWidth  / 2).coerceAtLeast(720)
        val captureHeight = (screenHeight / 2).coerceAtLeast(1280)

        imageReader = ImageReader.newInstance(
            captureWidth, captureHeight,
            PixelFormat.RGBA_8888, 3
        )

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "EdgeCapture",
            captureWidth, captureHeight, screenDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        // Start periodic frame capture
        startCapturing(fps, captureWidth, captureHeight, dir)

        return START_STICKY
    }

    private fun startCapturing(fps: Int, w: Int, h: Int, dir: File) {
        val intervalMs = (1000L / fps).coerceAtLeast(200L)
        serviceScope.launch {
            while (isActive && isRunning) {
                delay(intervalMs)
                captureFrame(dir)
            }
        }
    }

    private fun captureFrame(dir: File) {
        val image = imageReader?.acquireLatestImage() ?: return
        try {
            val planes     = image.planes
            val buffer     = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride   = planes[0].rowStride
            val rowPadding  = rowStride - pixelStride * image.width

            val bmp = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bmp.copyPixelsFromBuffer(buffer)

            // Crop to exact size
            val cropped = Bitmap.createBitmap(bmp, 0, 0, image.width, image.height)
            bmp.recycle()

            val file = File(dir, "frame_%05d.jpg".format(frameCount++))
            FileOutputStream(file).use { fos ->
                cropped.compress(Bitmap.CompressFormat.JPEG, 80, fos)
            }
            cropped.recycle()

        } catch (e: Exception) {
            Log.w(TAG, "Frame capture error: ${e.message}")
        } finally {
            image.close()
        }
    }

    override fun onDestroy() {
        isRunning = false
        serviceScope.cancel()
        virtualDisplay?.release()
        mediaProjection?.stop()
        imageReader?.close()
        virtualDisplay  = null
        mediaProjection = null
        imageReader     = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Edge Smartboard AI")
            .setContentText("Screen capture running — $frameCount frames captured")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Screen Capture",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Screen capture foreground service" }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}
