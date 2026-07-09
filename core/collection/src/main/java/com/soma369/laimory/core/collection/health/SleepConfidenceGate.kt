package com.soma369.laimory.core.collection.health

/**
 * 신뢰도 게이트(에픽 #142) — 감지 수면을 Health Connect 에 기록할 만큼 신뢰할 수 있는지 판정.
 *
 * 규칙: 구간이 [SleepDetectionStatus.DETECTED](잘 감지) 이고, 그 창의 신뢰도 표본 평균이
 * [MIN_AVERAGE_CONFIDENCE] 이상일 때만 기록한다. 데이터 공백([SleepDetectionStatus.MISSING_DATA])이나
 * 미감지([SleepDetectionStatus.NOT_DETECTED]), 표본이 없거나 평균이 낮은 밤은 기록하지 않고
 * 사용자 입력(#145)으로 넘어간다. GMS 무의존 순수 로직이라 JVM 테스트로 고정한다.
 */
internal object SleepConfidenceGate {
    /**
     * 기록해도 되는 감지인지.
     *
     * @param status 구간 감지 품질([SleepDetectionStatus]) — `SleepSegmentEvent.status` 매핑
     * @param confidences 구간 창에 속한 `SleepClassifyEvent` 신뢰도(0~100) 목록
     */
    fun shouldRecord(
        status: SleepDetectionStatus,
        confidences: List<Int>,
    ): Boolean =
        status == SleepDetectionStatus.DETECTED &&
            confidences.isNotEmpty() &&
            confidences.average() >= MIN_AVERAGE_CONFIDENCE

    /** 기록 임계값(구간 평균 신뢰도). 실기기 데이터로 튜닝 대상 — 잠정 기본값. */
    const val MIN_AVERAGE_CONFIDENCE: Double = 50.0
}
