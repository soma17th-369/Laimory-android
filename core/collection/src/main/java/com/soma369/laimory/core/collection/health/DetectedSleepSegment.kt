package com.soma369.laimory.core.collection.health

/**
 * 파싱된 수면 구간 한 건(Sleep API `SleepSegmentEvent` 에서 변환).
 *
 * [isSuccessful] 은 API status == `STATUS_SUCCESSFUL` — 데이터가 충분해 신뢰할 만한 감지였는지.
 * GMS 타입을 정책([SleepSegmentProcessor])에서 떼어내 순수 JVM 테스트가 되게 하는 경계 모델이다.
 */
internal data class DetectedSleepSegment(
    val startMillis: Long,
    val endMillis: Long,
    val isSuccessful: Boolean,
)
