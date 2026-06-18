package com.d3vk0.wardriving.rf.village.mx.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.d3vk0.wardriving.rf.village.mx.R
import com.d3vk0.wardriving.rf.village.mx.core.repository.UploadAttemptResult
import com.d3vk0.wardriving.rf.village.mx.core.repository.UploadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val uploadRepository: UploadRepository,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        Log.i(TAG, "UploadWorker started")
        return runCatching {
            uploadRepository.uploadAllPending()
        }.fold(
            onSuccess = { results ->
                results.filterIsInstance<UploadAttemptResult.Failure>()
                    .firstOrNull { it.actionable }
                    ?.let { notifyActionableFailure(it.message) }
                Log.i(TAG, "UploadWorker finished")
                Result.success()
            },
            onFailure = { error ->
                Log.e(TAG, "UploadWorker failed", error)
                Result.retry()
            },
        )
    }

    private fun notifyActionableFailure(message: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Upload errors", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle("API upload needs attention")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .build()
        manager.notify(ACTIONABLE_UPLOAD_ERROR_ID, notification)
    }

    private companion object {
        const val TAG = "UploadWorker"
        const val CHANNEL_ID = "upload_errors"
        const val ACTIONABLE_UPLOAD_ERROR_ID = 2002
    }
}
