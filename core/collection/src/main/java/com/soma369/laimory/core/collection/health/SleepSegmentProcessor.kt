package com.soma369.laimory.core.collection.health

import java.time.Instant
import java.time.ZoneId

/**
 * 감지된 수면 구간을 신뢰도 게이트 + 정합성(dedup) 판정 후 Health Connect 에 기록한다(에픽 #142).
 *
 * 밤 키는 기상(end) 로컬 날짜 기준 — 자정을 걸친 연속 밤이 같은 날짜 키로 충돌하지 않게 한다.
 * 그 밤에 외부 앱 수면이 이미 있으면([SleepHealthRecorder.hasExternalSleep]) 우리는 쓰지 않는다.
 */
internal class SleepSegmentProcessor(
    private val recorder: SleepHealthRecorder,
    private val classifyStore: SleepClassifyStore,
    private val zoneProvider: () -> ZoneId,
) {
    suspend fun process(segments: List<DetectedSleepSegment>) {
        val zone = zoneProvider()
        segments.forEach { segment -> process(segment, zone) }
    }

    private suspend fun process(
        segment: DetectedSleepSegment,
        zone: ZoneId,
    ) {
        val confidences = classifyStore.confidencesInWindow(segment.startMillis, segment.endMillis)
        if (!SleepConfidenceGate.shouldRecord(segment.isSuccessful, confidences)) return

        val start = Instant.ofEpochMilli(segment.startMillis)
        val end = Instant.ofEpochMilli(segment.endMillis)
        // 그 밤에 외부 앱 수면이 이미 있으면 우리는 쓰지 않는다(정합성 dedup).
        if (recorder.hasExternalSleep(start, end)) return
        recorder.recordDetectedSleep(
            night = end.atZone(zone).toLocalDate(),
            start = start,
            end = end,
            zoneOffset = zone.rules.getOffset(end),
        )
    }
}
