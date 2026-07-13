package com.soma369.laimory.core.collection.health.sleep.detection

import com.soma369.laimory.core.collection.health.sleep.record.SleepHealthRecorder
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
        // 저장된 표본에서 이 구간 창에 속한 신뢰도만 추린다(store 는 저장만, 창 필터는 여기서).
        val confidences =
            classifyStore
                .all()
                .filter { it.timestampMillis in segment.startMillis..segment.endMillis }
                .map { it.confidence }
        if (!SleepConfidenceGate.shouldRecord(segment.status, confidences)) return

        val start = Instant.ofEpochMilli(segment.startMillis)
        val end = Instant.ofEpochMilli(segment.endMillis)
        // 그 밤에 외부 앱 수면이 이미 있으면 우리는 쓰지 않는다(정합성 dedup).
        // 백그라운드 read 는 READ_HEALTH_DATA_IN_BACKGROUND 권한/지원이 없으면 실패할 수 있는데,
        // 그 경우 dedup 을 건너뛰고 기록은 진행한다(핵심 경로인 자동 기록을 잃지 않도록).
        if (hasExternalSleepOrFalse(start, end)) return
        recorder.recordDetectedSleep(
            night = end.atZone(zone).toLocalDate(),
            start = start,
            end = end,
            zoneOffset = zone.rules.getOffset(end),
        )
    }

    /** 외부 수면 존재 여부. 백그라운드 read 실패(권한·미지원) 시엔 dedup 을 포기하고 false 로 본다. */
    private suspend fun hasExternalSleepOrFalse(
        start: Instant,
        end: Instant,
    ): Boolean = runCatching { recorder.hasExternalSleep(start, end) }.getOrDefault(false)
}
