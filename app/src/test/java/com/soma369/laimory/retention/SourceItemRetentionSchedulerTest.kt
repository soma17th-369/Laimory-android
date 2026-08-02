package com.soma369.laimory.retention

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class SourceItemRetentionSchedulerTest {
    @Test
    fun `반복 호출해도 같은 unique name과 KEEP 정책으로 등록한다`() {
        val enqueuer = RecordingEnqueuer()
        val scheduler = SourceItemRetentionScheduler(enqueuer)

        scheduler.schedule()
        scheduler.schedule()

        assertEquals(2, enqueuer.invocations.size)
        enqueuer.invocations.forEach { invocation ->
            assertEquals(SourceItemRetentionScheduler.UNIQUE_WORK_NAME, invocation.uniqueWorkName)
            assertEquals(ExistingPeriodicWorkPolicy.KEEP, invocation.existingWorkPolicy)
        }
    }

    @Test
    fun `일일 작업에는 네트워크 제약을 두지 않는다`() {
        val enqueuer = RecordingEnqueuer()

        SourceItemRetentionScheduler(enqueuer).schedule()

        val workSpec = enqueuer.invocations.single().request.workSpec
        assertEquals(TimeUnit.DAYS.toMillis(1), workSpec.intervalDuration)
        assertEquals(NetworkType.NOT_REQUIRED, workSpec.constraints.requiredNetworkType)
    }

    private class RecordingEnqueuer : UniquePeriodicWorkEnqueuer {
        val invocations = mutableListOf<Invocation>()

        override fun enqueue(
            uniqueWorkName: String,
            existingWorkPolicy: ExistingPeriodicWorkPolicy,
            request: PeriodicWorkRequest,
        ) {
            invocations += Invocation(uniqueWorkName, existingWorkPolicy, request)
        }
    }

    private data class Invocation(
        val uniqueWorkName: String,
        val existingWorkPolicy: ExistingPeriodicWorkPolicy,
        val request: PeriodicWorkRequest,
    )
}
