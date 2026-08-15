package com.soma369.laimory.core.ui.component.timepicker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class LaimoryTimePickerMathTest {
    private val today: LocalDate = LocalDate.of(2026, 8, 1)
    private val tomorrow: LocalDate = today.plusDays(1)

    private val sameAndNextDay =
        listOf(
            TimePickerDateOption(today, "당일"),
            TimePickerDateOption(tomorrow, "익일"),
        )
    private val singleDay = listOf(TimePickerDateOption(today, "당일"))

    private fun value(
        hour: Int,
        minute: Int,
        date: LocalDate = today,
    ) = LaimoryTimePickerValue(date = date, time = LocalTime.of(hour, minute))

    @Test
    fun `분 선택지는 간격을 따르고 5분 간격은 12개다`() {
        assertEquals(12, TimePickerMinuteStep.FIVE.options().size)
        assertEquals(listOf(0, 5, 10), TimePickerMinuteStep.FIVE.options().take(3))
        assertEquals(60, TimePickerMinuteStep.ONE.options().size)
    }

    @Test
    fun `간격에 없는 기존 분은 선택지에 끼워 넣어 값을 보존한다`() {
        val options = LaimoryTimePickerMath.minuteOptions(TimePickerMinuteStep.FIVE, currentMinute = 37)

        assertTrue(options.contains(37))
        assertEquals(13, options.size)
        assertEquals(options.sorted(), options)
    }

    @Test
    fun `간격에 있는 분은 선택지를 늘리지 않는다`() {
        val options = LaimoryTimePickerMath.minuteOptions(TimePickerMinuteStep.FIVE, currentMinute = 35)

        assertEquals(TimePickerMinuteStep.FIVE.options(), options)
    }

    @Test
    fun `분 롤러는 순환하며 시로 올림한다`() {
        val wrapped =
            LaimoryTimePickerMath.scrollMinute(
                value = value(hour = 10, minute = 55),
                delta = 1,
                step = TimePickerMinuteStep.FIVE,
                dates = sameAndNextDay,
            )

        assertEquals(value(hour = 11, minute = 0), wrapped)
    }

    @Test
    fun `분 롤러 역방향 순환은 시를 내린다`() {
        val wrapped =
            LaimoryTimePickerMath.scrollMinute(
                value = value(hour = 10, minute = 0),
                delta = -1,
                step = TimePickerMinuteStep.FIVE,
                dates = sameAndNextDay,
            )

        assertEquals(value(hour = 9, minute = 55), wrapped)
    }

    @Test
    fun `날짜 열이 있으면 자정을 넘을 때 날짜가 함께 이동한다`() {
        val forward = LaimoryTimePickerMath.scrollHour(value(hour = 23, minute = 30), delta = 1, dates = sameAndNextDay)
        val backward =
            LaimoryTimePickerMath.scrollHour(
                value(hour = 0, minute = 30, date = tomorrow),
                delta = -1,
                dates = sameAndNextDay,
            )

        assertEquals(value(hour = 0, minute = 30, date = tomorrow), forward)
        assertEquals(value(hour = 23, minute = 30), backward)
    }

    @Test
    fun `분에서 시작한 순환도 자정을 넘으면 날짜로 이어진다`() {
        val carried =
            LaimoryTimePickerMath.scrollMinute(
                value = value(hour = 23, minute = 55),
                delta = 1,
                step = TimePickerMinuteStep.FIVE,
                dates = sameAndNextDay,
            )

        assertEquals(value(hour = 0, minute = 0, date = tomorrow), carried)
    }

    @Test
    fun `날짜 열이 없으면 시는 순환하되 날짜는 그대로다`() {
        val forward = LaimoryTimePickerMath.scrollHour(value(hour = 23, minute = 30), delta = 1, dates = singleDay)
        val backward = LaimoryTimePickerMath.scrollHour(value(hour = 0, minute = 30), delta = -1, dates = singleDay)

        assertEquals(value(hour = 0, minute = 30), forward)
        assertEquals(value(hour = 23, minute = 30), backward)
    }

    @Test
    fun `날짜 롤러는 목록 경계에서 멈춘다`() {
        val beyondEnd = LaimoryTimePickerMath.shiftDate(value(hour = 9, minute = 0, date = tomorrow), delta = 1, dates = sameAndNextDay)
        val beforeStart = LaimoryTimePickerMath.shiftDate(value(hour = 9, minute = 0), delta = -1, dates = sameAndNextDay)

        assertEquals(tomorrow, beyondEnd.date)
        assertEquals(today, beforeStart.date)
    }

    @Test
    fun `빠른 조정은 스냅 없이 정확히 더한다`() {
        val adjusted =
            LaimoryTimePickerMath.quickAdjust(value(hour = 10, minute = 37), minutes = 5, dates = sameAndNextDay)

        assertEquals(value(hour = 10, minute = 42), adjusted)
    }

    @Test
    fun `빠른 조정이 자정을 넘으면 허용된 다음 날짜로 이동한다`() {
        val adjusted =
            LaimoryTimePickerMath.quickAdjust(value(hour = 23, minute = 30), minutes = 60, dates = sameAndNextDay)

        assertEquals(value(hour = 0, minute = 30, date = tomorrow), adjusted)
    }

    @Test
    fun `빠른 조정 결과가 허용 날짜를 벗어나면 null을 반환해 버튼을 막는다`() {
        val beyondLastDate =
            LaimoryTimePickerMath.quickAdjust(
                value(hour = 23, minute = 30, date = tomorrow),
                minutes = 60,
                dates = sameAndNextDay,
            )
        val beyondSingleDate =
            LaimoryTimePickerMath.quickAdjust(value(hour = 23, minute = 30), minutes = 60, dates = singleDay)

        assertNull(beyondLastDate)
        assertNull(beyondSingleDate)
    }

    @Test
    fun `표시용 선택지를 만들어도 값 자체는 변하지 않는다`() {
        val original = value(hour = 10, minute = 37)

        LaimoryTimePickerMath.minuteOptions(TimePickerMinuteStep.FIVE, original.time.minute)

        assertEquals(37, original.time.minute)
    }
}
