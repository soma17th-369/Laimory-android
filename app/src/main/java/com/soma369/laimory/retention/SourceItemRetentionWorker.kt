package com.soma369.laimory.retention

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.soma369.laimory.core.domain.usecase.DeleteExpiredSourceItemsUseCase
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

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
