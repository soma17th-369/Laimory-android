package com.soma369.laimory.core.collection.health

/**
 * 신뢰도 게이트(에픽 #142) — 감지 수면을 Health Connect 에 기록할 만큼 신뢰할 수 있는지 판정.
 *
 * 규칙: 구간 status 가 성공이고, 그 창의 신뢰도 표본 평균이 [MIN_AVERAGE_CONFIDENCE] 이상일 때만 기록한다.
 * 표본이 없으면(신뢰도 확인 불가) 기록하지 않는다 — 낮거나 불확실한 밤은 사용자 입력(#145)으로 넘어간다.
 * GMS 무의존 순수 로직이라 JVM 테스트로 고정한다.
 */
internal object SleepConfidenceGate {
    /**
     * 기록해도 되는 신뢰도인지.
     *
     * @param isSuccessful `SleepSegmentEvent.status == STATUS_SUCCESSFUL`
     * @param confidences 구간 창에 속한 `SleepClassifyEvent` 신뢰도(0~100) 목록
     */
    fun shouldRecord(
        isSuccessful: Boolean,
        confidences: List<Int>,
    ): Boolean = isSuccessful && confidences.isNotEmpty() && confidences.average() >= MIN_AVERAGE_CONFIDENCE

    /** 기록 임계값(구간 평균 신뢰도). 실기기 데이터로 튜닝 대상 — 잠정 기본값. */
    const val MIN_AVERAGE_CONFIDENCE: Double = 50.0
}
