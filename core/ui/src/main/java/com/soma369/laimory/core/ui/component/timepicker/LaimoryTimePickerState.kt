package com.soma369.laimory.core.ui.component.timepicker

import androidx.compose.runtime.Immutable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 타임 피커 날짜 롤러의 한 항목.
 *
 * [date]는 실제 날짜, [label]은 화면 표시명이다. `당일`·`익일` 같은 상대 표현은 제품 정책이므로
 * 컴포넌트가 만들지 않고 호출부가 제공한다.
 */
@Immutable
data class TimePickerDateOption(
    val date: LocalDate,
    val label: String,
)

/** 분 롤러 간격. 새 값을 입력하는 화면은 [FIVE], 기존 서버 값을 편집하는 화면은 [ONE]을 쓴다. */
enum class TimePickerMinuteStep(
    val minutes: Int,
) {
    ONE(1),
    FIVE(5),
    ;

    /** [minute]가 이 간격의 선택지에 포함되는지. */
    fun contains(minute: Int): Boolean = minute % minutes == 0

    /** 이 간격으로 노출할 분 선택지. */
    fun options(): List<Int> = (0 until MINUTES_PER_HOUR step minutes).toList()
}

/**
 * 타임 피커가 다루는 날짜·시각 한 쌍.
 *
 * 표시·펼침만으로는 값을 바꾸지 않는다 — [minuteOptions]가 현재 분을 포함하지 않아도(1분 값을 5분
 * 간격으로 여는 등) 사용자가 분을 실제로 고르기 전까지 원래 값을 유지한다.
 */
@Immutable
data class LaimoryTimePickerValue(
    val date: LocalDate,
    val time: LocalTime,
) {
    val dateTime: LocalDateTime get() = LocalDateTime.of(date, time)

    companion object {
        fun of(dateTime: LocalDateTime): LaimoryTimePickerValue =
            LaimoryTimePickerValue(date = dateTime.toLocalDate(), time = dateTime.toLocalTime())
    }
}

/**
 * 타임 피커의 순수 계산.
 *
 * Android 의존이 없어 JVM 테스트로 검증한다. 롤러 순환과 자정 carry, 빠른 조정의 허용 범위 판정을
 * 모두 이곳에서 결정하고, Composable은 결과를 표시만 한다.
 *
 * 자정 carry 규칙:
 * - 분이 한 바퀴 돌면 시가 ±1, 시가 한 바퀴 돌면 날짜가 ±1로 이어진다(시계 자릿수와 동일).
 * - 날짜 롤러가 없으면(선택지 1개) 옮길 날짜가 없으므로 시·분만 순환하고 날짜는 그대로 둔다.
 * - 날짜 롤러가 있어도 목록의 처음·끝을 넘어서는 이동은 하지 않는다.
 */
internal object LaimoryTimePickerMath {
    /** 시 선택지(0~23). */
    fun hourOptions(): List<Int> = (0 until HOURS_PER_DAY).toList()

    /**
     * 현재 값이 [step] 선택지에 없더라도 롤러에 표시할 분 목록.
     *
     * 기존 서버 값(예: 5분 간격 화면의 37분)을 잃지 않도록 현재 값을 목록에 끼워 넣는다.
     */
    fun minuteOptions(
        step: TimePickerMinuteStep,
        currentMinute: Int,
    ): List<Int> {
        val options = step.options()
        return if (step.contains(currentMinute)) options else (options + currentMinute).sorted()
    }

    /**
     * 분 롤러를 [delta]칸 이동한 결과. 목록 끝을 넘으면 순환하고 시로 올림한다.
     *
     * @param dates 날짜 롤러 선택지. 비어 있거나 1개면 날짜는 이동하지 않는다.
     */
    fun scrollMinute(
        value: LaimoryTimePickerValue,
        delta: Int,
        step: TimePickerMinuteStep,
        dates: List<TimePickerDateOption>,
    ): LaimoryTimePickerValue {
        val options = minuteOptions(step, value.time.minute)
        val currentIndex = options.indexOf(value.time.minute)
        val rawIndex = currentIndex + delta
        val nextIndex = Math.floorMod(rawIndex, options.size)
        val hourCarry = Math.floorDiv(rawIndex, options.size)
        val carried =
            value.copy(time = value.time.withMinute(options[nextIndex]))
        return if (hourCarry == 0) carried else scrollHour(carried, hourCarry, dates)
    }

    /** 시 롤러를 [delta]칸 이동한 결과. 하루를 넘으면 순환하고 날짜로 올림한다. */
    fun scrollHour(
        value: LaimoryTimePickerValue,
        delta: Int,
        dates: List<TimePickerDateOption>,
    ): LaimoryTimePickerValue {
        val rawHour = value.time.hour + delta
        val nextHour = Math.floorMod(rawHour, HOURS_PER_DAY)
        val dayCarry = Math.floorDiv(rawHour, HOURS_PER_DAY)
        val withHour = value.copy(time = value.time.withHour(nextHour))
        return if (dayCarry == 0) withHour else shiftDate(withHour, dayCarry, dates)
    }

    /** 날짜 롤러를 [delta]칸 이동한 결과. 목록 경계를 넘으면 이동하지 않는다. */
    fun shiftDate(
        value: LaimoryTimePickerValue,
        delta: Int,
        dates: List<TimePickerDateOption>,
    ): LaimoryTimePickerValue {
        if (dates.size <= 1) return value
        val currentIndex = dates.indexOfFirst { it.date == value.date }
        if (currentIndex < 0) return value
        val nextIndex = (currentIndex + delta).coerceIn(0, dates.lastIndex)
        return value.copy(date = dates[nextIndex].date)
    }

    /**
     * 빠른 조정 결과. 스냅 없이 정확히 [minutes]를 더하고, 허용 범위를 벗어나면 null을 반환한다.
     *
     * 범위 밖이면 버튼을 비활성화하기 위한 신호다 — 클램프하거나 임의로 보정하지 않는다.
     */
    fun quickAdjust(
        value: LaimoryTimePickerValue,
        minutes: Long,
        dates: List<TimePickerDateOption>,
    ): LaimoryTimePickerValue? {
        val adjusted = LaimoryTimePickerValue.of(value.dateTime.plusMinutes(minutes))
        val allowedDates = dates.map(TimePickerDateOption::date)
        val isWithinDates = if (allowedDates.isEmpty()) adjusted.date == value.date else adjusted.date in allowedDates
        return adjusted.takeIf { isWithinDates }
    }

    private const val HOURS_PER_DAY = 24
}

private const val MINUTES_PER_HOUR = 60
