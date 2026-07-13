package com.soma369.laimory.core.collection.health.sleep.detection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 감지 단계별 기록 판정을 고정한다.
 * - 잘 감지됨(DETECTED) + 신뢰도 충분 → 기록
 * - 잘 감지 안 됨(DETECTED 이지만 신뢰도 미달 / MISSING_DATA) → 미기록
 * - 아예 감지 안 됨(NOT_DETECTED) → 미기록
 */
class SleepConfidenceGateTest {
    // --- 잘 감지됨: 기록 ---

    @Test
    fun `잘 감지되고 평균 신뢰도가 임계값 이상이면 기록한다`() {
        assertTrue(SleepConfidenceGate.shouldRecord(SleepDetectionStatus.DETECTED, listOf(60, 70, 80)))
    }

    @Test
    fun `평균 신뢰도가 임계값과 정확히 같으면 기록한다`() {
        // 경계: 평균 50.0 == MIN_AVERAGE_CONFIDENCE
        assertTrue(SleepConfidenceGate.shouldRecord(SleepDetectionStatus.DETECTED, listOf(50, 50)))
    }

    // --- 잘 감지 안 됨: 미기록(불확실 → 사용자 입력) ---

    @Test
    fun `잘 감지됐어도 평균 신뢰도가 임계값 미만이면 기록하지 않는다`() {
        assertFalse(SleepConfidenceGate.shouldRecord(SleepDetectionStatus.DETECTED, listOf(10, 20, 30)))
    }

    @Test
    fun `평균 신뢰도가 임계값을 살짝 밑돌면 기록하지 않는다`() {
        // 경계 바로 아래: 평균 49.5 < 50.0
        assertFalse(SleepConfidenceGate.shouldRecord(SleepDetectionStatus.DETECTED, listOf(49, 50)))
    }

    @Test
    fun `신뢰도 표본이 없으면 기록하지 않는다`() {
        assertFalse(SleepConfidenceGate.shouldRecord(SleepDetectionStatus.DETECTED, emptyList()))
    }

    @Test
    fun `데이터 공백 구간은 신뢰도가 높아도 기록하지 않는다`() {
        assertFalse(SleepConfidenceGate.shouldRecord(SleepDetectionStatus.MISSING_DATA, listOf(90, 95)))
    }

    // --- 아예 감지 안 됨: 미기록 ---

    @Test
    fun `감지되지 않은 구간은 신뢰도가 높아도 기록하지 않는다`() {
        assertFalse(SleepConfidenceGate.shouldRecord(SleepDetectionStatus.NOT_DETECTED, listOf(90, 95)))
    }
}
