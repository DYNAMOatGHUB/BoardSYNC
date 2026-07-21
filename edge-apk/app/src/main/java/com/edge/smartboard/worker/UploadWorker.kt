package com.edge.smartboard.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker for background ZIP upload with auto-resume.
 */
@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_SESSION_ID  = "session_id"
        const val KEY_FILE_PATH   = "file_path"
        const val CHANNEL_ID      = "upload_worker_channel"
        const val NOTIF_ID        = 2001

        fun enqueue(context: Context, sessionId: String, filePath: String): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(false)
                .build()

            val data = workDataOf(
                KEY_SESSION_ID to sessionId,
                KEY_FILE_PATH  to filePath
            )

            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("upload_$sessionId")
                .build()

            return WorkManager.getInstance(context)
                .enqueueUniqueWork("upload_$sessionId", ExistingWorkPolicy.REPLACE, request)
        }
    }

    override suspend fun doWork(): Result {
        val sessionId = inputData.getString(KEY_SESSION_ID) ?: return Result.failure()
        val filePath  = inputData.getString(KEY_FILE_PATH)  ?: return Result.failure()

        createNotificationChannel()
        setForeground(createForegroundInfo(sessionId, 0))

        return try {
            // Simulate upload steps
            for (progress in 0..100 step 10) {
                setForeground(createForegroundInfo(sessionId, progress))
                delay(500L)
            }
            // TODO: Call repository.uploadSession(zipFile, sessionId, metadata)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry()
            else Result.failure()
        }
    }

    private fun createForegroundInfo(sessionId: String, progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Uploading Session")
            .setContentText("$sessionId — $progress%")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIF_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Upload Service", NotificationManager.IMPORTANCE_LOW
        )
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
