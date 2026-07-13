package com.soma369.laimory.core.collection.health.sleep.record

import java.time.Instant
import java.time.ZoneOffset

/**
 * Health Connect 에 쓸 수면 세션 한 건.
 *
 * 같은 [clientRecordId] 로 다시 쓰면 새 레코드를 추가하지 않고 기존 것을 덮어쓴다(멱등 upsert).
 * 재기록 시 [version] 이 기존 값 이상이어야 교체되므로, 밤의 종료 시각(epoch millis)을 실어
 * 나중 감지가 이전 것을 갱신하게 한다.
 */
internal data class SleepWrite(
    val clientRecordId: String,
    val start: Instant,
    val end: Instant,
    val zoneOffset: ZoneOffset?,
    val version: Long,
    val recordingMethod: SleepRecordingMethod,
)
