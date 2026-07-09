package com.soma369.laimory.core.collection.health

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepConfidenceGateTest {
    @Test
    fun `성공 구간이고 평균 신뢰도가 임계값 이상이면 기록한다`() {
        assertTrue(SleepConfidenceGate.shouldRecord(isSuccessful = true, confidences = listOf(60, 70, 80)))
    }

    @Test
    fun `평균 신뢰도가 임계값 미만이면 기록하지 않는다`() {
        assertFalse(SleepConfidenceGate.shouldRecord(isSuccessful = true, confidences = listOf(10, 20, 30)))
    }

    @Test
    fun `status 가 성공이 아니면 신뢰도가 높아도 기록하지 않는다`() {
        assertFalse(SleepConfidenceGate.shouldRecord(isSuccessful = false, confidences = listOf(90, 95)))
    }

    @Test
    fun `신뢰도 표본이 없으면 기록하지 않는다`() {
        assertFalse(SleepConfidenceGate.shouldRecord(isSuccessful = true, confidences = emptyList()))
    }
}
