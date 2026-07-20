package com.soma369.laimory.feature.home.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.soma369.laimory.feature.home.state.DraftEndDay
import com.soma369.laimory.feature.home.state.HomeUiState
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal fun HomeUiState.timeRangeLabel(): String {
    val endPrefix = if (endDay == DraftEndDay.NEXT_DAY) "익일 " else ""
    return "${startTime.format(TIME_FORMAT)} ~ $endPrefix${endTime.format(TIME_FORMAT)}"
}

@Composable
internal fun rememberToday(zone: ZoneId = ZoneId.systemDefault()): LocalDate {
    val today by
        produceState(initialValue = LocalDate.now(zone), zone) {
            while (true) {
                val now = ZonedDateTime.now(zone)
                value = now.toLocalDate()
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zone)
                delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L))
            }
        }
    return today
}

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
