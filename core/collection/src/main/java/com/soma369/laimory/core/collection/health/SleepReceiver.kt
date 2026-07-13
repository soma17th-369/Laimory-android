package com.soma369.laimory.core.collection.health

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.SleepClassifyEvent
import com.google.android.gms.location.SleepSegmentEvent
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Sleep API 이벤트 수신. 신뢰도 표본은 저장하고, 수면 구간은 게이트+정합성 판정 후 HC 기록으로 넘긴다.
 *
 * [SleepDetectionSubscriber] 가 등록한 PendingIntent 로만 트리거된다. 처리(DataStore/HC)가 suspend 라
 * `goAsync` 로 브로드캐스트를 잡아두고 IO 코루틴에서 마친다. 의존성은 [EntryPointAccessors] 로 얻는다.
 */
internal class SleepReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface Entry {
        fun classifyStore(): SleepClassifyStore

        fun segmentProcessor(): SleepSegmentProcessor
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val samples =
            if (SleepClassifyEvent.hasEvents(intent)) {
                SleepClassifyEvent.extractEvents(intent).map { SleepClassifySample(it.timestampMillis, it.confidence) }
            } else {
                emptyList()
            }
        val segments =
            if (SleepSegmentEvent.hasEvents(intent)) {
                SleepSegmentEvent.extractEvents(intent).map {
                    DetectedSleepSegment(
                        startMillis = it.startTimeMillis,
                        endMillis = it.endTimeMillis,
                        status = detectionStatusOf(it.status),
                    )
                }
            } else {
                emptyList()
            }
        if (samples.isEmpty() && segments.isEmpty()) return

        val entry = EntryPointAccessors.fromApplication(context.applicationContext, Entry::class.java)
        val pending = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            runCatching {
                if (samples.isNotEmpty()) entry.classifyStore().add(samples)
                if (segments.isNotEmpty()) entry.segmentProcessor().process(segments)
            }.onFailure { e -> Logger.w(LogDomain.COLLECTION, "수면 이벤트 처리 실패: ${e.message}") }
            pending.finish()
            scope.cancel()
        }
    }

    /** GMS `SleepSegmentEvent.status` 를 감지 품질 모델로 매핑한다. */
    private fun detectionStatusOf(status: Int): SleepDetectionStatus =
        when (status) {
            SleepSegmentEvent.STATUS_SUCCESSFUL -> SleepDetectionStatus.DETECTED
            SleepSegmentEvent.STATUS_MISSING_DATA -> SleepDetectionStatus.MISSING_DATA
            else -> SleepDetectionStatus.NOT_DETECTED
        }
}
