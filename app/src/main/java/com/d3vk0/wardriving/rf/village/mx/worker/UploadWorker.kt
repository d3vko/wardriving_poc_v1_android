package com.d3vk0.wardriving.rf.village.mx.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
            onSuccess = {
                Log.i(TAG, "UploadWorker finished")
                Result.success()
            },
            onFailure = { error ->
                Log.e(TAG, "UploadWorker failed", error)
                Result.retry()
            },
        )
    }

    private companion object {
        const val TAG = "UploadWorker"
    }
}
