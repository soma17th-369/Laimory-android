package com.soma369.laimory.feature.home.component

import com.soma369.laimory.feature.home.state.DraftEndDay
import com.soma369.laimory.feature.home.state.HomeDatePickerSession
import com.soma369.laimory.feature.home.state.HomeUiState
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal fun HomeUiState.timeRangeLabel(): String = timeRangeLabel(startTime, endDay, endTime)

/** 피커 안에서는 확정 전의 임시 범위를 적는다. */
internal fun HomeDatePickerSession.timeRangeLabel(): String = timeRangeLabel(startTime, endDay, endTime)

private fun timeRangeLabel(
    startTime: LocalTime,
    endDay: DraftEndDay,
    endTime: LocalTime,
): String {
    val endPrefix = if (endDay == DraftEndDay.NEXT_DAY) "익일 " else ""
    return "${startTime.format(TIME_FORMAT)} ~ $endPrefix${endTime.format(TIME_FORMAT)}"
}

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
