package com.soma369.laimory.retention

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.soma369.laimory.core.domain.usecase.DeleteExpiredSourceItemsUseCase
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** 앱 시작마다 같은 이름의 일일 정리 작업을 보장한다. */
@Singleton
class SourceItemRetentionScheduler
    @Inject
    internal constructor(
        private val enqueuer: UniquePeriodicWorkEnqueuer,
    ) {
        fun schedule() {
            try {
                enqueuer.enqueue(
                    uniqueWorkName = UNIQUE_WORK_NAME,
                    existingWorkPolicy = ExistingPeriodicWorkPolicy.KEEP,
                    request =
                        PeriodicWorkRequestBuilder<SourceItemRetentionWorker>(
                            REPEAT_INTERVAL_DAYS,
                            TimeUnit.DAYS,
                        ).build(),
                )
            } catch (error: Exception) {
                Logger.e(LogDomain.COLLECTION, "SourceItem 보존 작업 예약 실패; 다음 앱 시작에서 재시도", error)
            }
        }

        internal companion object {
            const val UNIQUE_WORK_NAME = "source-item-retention"
            const val REPEAT_INTERVAL_DAYS = 1L
        }
    }

internal interface UniquePeriodicWorkEnqueuer {
    fun enqueue(
        uniqueWorkName: String,
        existingWorkPolicy: ExistingPeriodicWorkPolicy,
        request: PeriodicWorkRequest,
    )
}

@Singleton
internal class WorkManagerUniquePeriodicWorkEnqueuer
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : UniquePeriodicWorkEnqueuer {
        override fun enqueue(
            uniqueWorkName: String,
            existingWorkPolicy: ExistingPeriodicWorkPolicy,
            request: PeriodicWorkRequest,
        ) {
            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(uniqueWorkName, existingWorkPolicy, request)
        }
    }

/**
 * 만료된 SourceItem만 정리하는 일일 Worker.
 *
 * 정리는 best-effort 작업이므로 실패해도 즉시 재시도하지 않는다. 실패를 로그로 남기고 성공으로 종료해
 * 앱 시작과 수집을 방해하지 않으며, 다음 일일 실행에서 다시 정리한다.
 */
@HiltWorker
class SourceItemRetentionWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParameters: WorkerParameters,
        private val deleteExpiredSourceItems: DeleteExpiredSourceItemsUseCase,
    ) : CoroutineWorker(appContext, workerParameters) {
        override suspend fun doWork(): Result =
            try {
                val deletedCount = deleteExpiredSourceItems()
                Logger.i(LogDomain.COLLECTION, "SourceItem 보존 정리 완료(deletedCount=$deletedCount)")
                Result.success()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Logger.e(LogDomain.COLLECTION, "SourceItem 보존 정리 실패; 다음 일일 실행에서 재시도", error)
                Result.success()
            }
    }
