package com.soma369.laimory.retention

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 시작마다 같은 이름의 일일 정리 작업을 보장한다.
 *
 * [ExistingPeriodicWorkPolicy.KEEP]은 이미 등록된 작업의 주기와 제약을 갱신하지 않는다.
 * 해당 설정을 변경할 때는 `UPDATE` 정책으로의 전환을 함께 검토해야 한다.
 */
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
