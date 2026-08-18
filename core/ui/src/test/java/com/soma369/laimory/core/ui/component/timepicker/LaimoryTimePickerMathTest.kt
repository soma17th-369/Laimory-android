package com.soma369.laimory.core.ui.component.timepicker

import org.junit.Assert.assertEquals
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
    fun `표시용 선택지를 만들어도 값 자체는 변하지 않는다`() {
        val original = value(hour = 10, minute = 37)

        LaimoryTimePickerMath.minuteOptions(TimePickerMinuteStep.FIVE, original.time.minute)

        assertEquals(37, original.time.minute)
    }

    @Test
    fun `자정을 넘길 때 시 롤러는 한 칸만 앞으로 간다`() {
        // 시 열: 선택지 24개를 201주기 반복, 지금 자리는 23시(2423).
        val target =
            LaimoryTimePickerMath.nearestIndexOf(
                selectedIndex = 0,
                current = 2423,
                optionCount = 24,
                itemCount = 24 * 201,
            )

        assertEquals(2424, target)
    }

    @Test
    fun `자정을 거슬러 올라갈 때도 시 롤러는 한 칸만 뒤로 간다`() {
        val target =
            LaimoryTimePickerMath.nearestIndexOf(
                selectedIndex = 23,
                current = 2400,
                optionCount = 24,
                itemCount = 24 * 201,
            )

        assertEquals(2399, target)
    }

    @Test
    fun `순환하지 않는 열은 목록 안의 자리만 고른다`() {
        val toNextDay =
            LaimoryTimePickerMath.nearestIndexOf(selectedIndex = 1, current = 0, optionCount = 2, itemCount = 2)
        val toSameDay =
            LaimoryTimePickerMath.nearestIndexOf(selectedIndex = 0, current = 1, optionCount = 2, itemCount = 2)

        assertEquals(1, toNextDay)
        assertEquals(0, toSameDay)
    }

    @Test
    fun `자정을 넘지 않는 이동은 제자리 주기를 그대로 쓴다`() {
        val target =
            LaimoryTimePickerMath.nearestIndexOf(
                selectedIndex = 17,
                current = 2416,
                optionCount = 24,
                itemCount = 24 * 201,
            )

        assertEquals(2417, target)
    }

    @Test
    fun `범위 밖 분은 선택지에서 빠진다`() {
        val range = today.atTime(6, 0)..tomorrow.atTime(6, 0)

        val atLowerBound = LaimoryTimePickerMath.allowedMinutes(today, 6, TimePickerMinuteStep.FIVE, 0, range)
        val atUpperBound = LaimoryTimePickerMath.allowedMinutes(tomorrow, 6, TimePickerMinuteStep.FIVE, 0, range)

        assertEquals(TimePickerMinuteStep.FIVE.options(), atLowerBound)
        // 익일 06:00 이 상한이라 그 시각에는 0분만 남는다.
        assertEquals(listOf(0), atUpperBound)
    }

    @Test
    fun `고를 분이 없는 시는 시 선택지에서도 빠진다`() {
        val range = today.atTime(6, 0)..tomorrow.atTime(6, 0)

        val sameDayHours = LaimoryTimePickerMath.allowedHours(today, TimePickerMinuteStep.FIVE, 0, range)
        val nextDayHours = LaimoryTimePickerMath.allowedHours(tomorrow, TimePickerMinuteStep.FIVE, 0, range)

        assertEquals((6..23).toList(), sameDayHours)
        assertEquals((0..6).toList(), nextDayHours)
    }

    @Test
    fun `고를 시각이 하나도 없는 날짜는 날짜 선택지에서 빠진다`() {
        val dates =
            listOf(
                TimePickerDateOption(today, "당일"),
                TimePickerDateOption(tomorrow, "익일"),
            )
        // 시작이 늦어 종료 하한이 익일로 밀린 경우.
        val range = tomorrow.atTime(5, 55)..tomorrow.atTime(6, 0)

        val allowed = LaimoryTimePickerMath.allowedDates(dates, TimePickerMinuteStep.FIVE, 0, range)

        assertEquals(listOf(tomorrow), allowed.map(TimePickerDateOption::date))
    }

    @Test
    fun `범위가 있는 열은 끝에서 멈춘다`() {
        val hours = listOf(0, 1, 2, 3, 4, 5, 6)

        assertEquals(0, LaimoryTimePickerMath.scrollWithin(hours, current = 0, delta = -3))
        assertEquals(6, LaimoryTimePickerMath.scrollWithin(hours, current = 6, delta = 3))
        assertEquals(3, LaimoryTimePickerMath.scrollWithin(hours, current = 2, delta = 1))
    }

    @Test
    fun `목록에 없는 값은 가장 가까운 선택지를 기준으로 옮긴다`() {
        val hours = listOf(6, 7, 8)

        // 범위가 좁아져 12시가 사라진 뒤 굴리면 가장 가까운 8시에서 출발한다.
        assertEquals(8, LaimoryTimePickerMath.scrollWithin(hours, current = 12, delta = 0))
        assertEquals(7, LaimoryTimePickerMath.scrollWithin(hours, current = 12, delta = -1))
    }

    @Test
    fun `범위 밖 값은 가까운 경계로 붙는다`() {
        val range = today.atTime(6, 0)..tomorrow.atTime(6, 0)

        assertEquals(
            LaimoryTimePickerValue.of(today.atTime(6, 0)),
            LaimoryTimePickerMath.coerceIntoRange(LaimoryTimePickerValue.of(today.atTime(3, 0)), range),
        )
        assertEquals(
            LaimoryTimePickerValue.of(tomorrow.atTime(6, 0)),
            LaimoryTimePickerMath.coerceIntoRange(LaimoryTimePickerValue.of(tomorrow.atTime(9, 0)), range),
        )
        val inside = LaimoryTimePickerValue.of(today.atTime(20, 0))
        assertEquals(inside, LaimoryTimePickerMath.coerceIntoRange(inside, range))
    }
}
