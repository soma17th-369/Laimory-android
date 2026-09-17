package com.soma369.laimory.feature.home.component

import com.soma369.laimory.feature.home.state.DraftEndDay
import com.soma369.laimory.feature.home.state.HomeUiState
import java.time.format.DateTimeFormatter

internal fun HomeUiState.timeRangeLabel(): String {
    val endPrefix = if (endDay == DraftEndDay.NEXT_DAY) "익일 " else ""
    return "${startTime.format(TIME_FORMAT)} ~ $endPrefix${endTime.format(TIME_FORMAT)}"
}

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
