package com.soma369.laimory.core.collection.health

/**
 * 수면 신뢰도 표본 한 건(Sleep API `SleepClassifyEvent` 에서 변환).
 *
 * [confidence] 는 0~100, 높을수록 "자는 중"에 대한 확신이 크다. 하루 내내 ~10분 간격으로 도착하며,
 * 구간([DetectedSleepSegment])이 도착했을 때 그 창의 표본을 모아 신뢰도 게이트를 판정한다.
 */
internal data class SleepClassifySample(
    val timestampMillis: Long,
    val confidence: Int,
)
