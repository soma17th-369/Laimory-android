package com.soma369.laimory.core.collection.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationMapperTest {
    @Test
    fun `진행률이 남아 있어도 완료 상태는 진행 중으로 보지 않는다`() {
        assertFalse(isProgressOngoing(max = 100, progress = 100, indeterminate = false))
    }

    @Test
    fun `진행 표시를 끈 알림은 진행 중으로 보지 않는다`() {
        // setProgress(0, 0, false) 로 표시를 해제해도 extras 키는 남는다.
        assertFalse(isProgressOngoing(max = 0, progress = 0, indeterminate = false))
    }

    @Test
    fun `진행률이 최대에 못 미치면 진행 중으로 본다`() {
        assertTrue(isProgressOngoing(max = 100, progress = 40, indeterminate = false))
    }

    @Test
    fun `불확정 진행률은 최대값과 무관하게 진행 중으로 본다`() {
        assertTrue(isProgressOngoing(max = 0, progress = 0, indeterminate = true))
    }
}
