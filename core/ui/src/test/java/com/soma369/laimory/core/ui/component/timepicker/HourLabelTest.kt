package com.soma369.laimory.core.ui.component.timepicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class HourLabelTest {
    @Test
    fun `정시는 시만 쓴다`() {
        assertEquals("21시", LocalTime.of(21, 0).toHourLabel())
        assertEquals("9시", LocalTime.of(9, 0).toHourLabel())
    }

    @Test
    fun `분이 있으면 내림하지 않고 분까지 쓴다`() {
        // 정시 피커 이전에 저장된 값. 내림해 보이면 실제와 다른 시각을 말한다.
        assertEquals("21시 5분", LocalTime.of(21, 5).toHourLabel())
    }

    @Test
    fun `정시 간격은 00분 하나만 고를 수 있다`() {
        assertEquals(listOf(0), TimePickerMinuteStep.HOUR.options())
        assertTrue(TimePickerMinuteStep.HOUR.contains(0))
        assertFalse(TimePickerMinuteStep.HOUR.contains(30))
    }
}
