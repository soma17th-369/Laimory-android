package com.soma369.laimory.feature.timeline.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.ui.base.UiState
import com.soma369.laimory.feature.timeline.model.CalendarRecordUiModel
import java.time.LocalDate
import java.time.YearMonth

@Immutable
data class CalendarUiState(
    /** 표시 중인 월. 날짜 선택으로는 바뀌지 않고 월 전환으로만 바뀐다. */
    val visibleMonth: YearMonth,
    val selectedDate: LocalDate,
    /** 기준 오늘. ON_RESUME 동기화마다 재계산해 자정 경과를 반영한다. */
    val today: LocalDate,
    val content: CalendarRecordsUiContent = CalendarRecordsUiContent.Loading,
) : UiState {
    /** 조회 전·실패 상태에서는 빈 맵이라 모든 날짜가 "기록 없음"으로 다뤄진다. */
    val recordsByDate: Map<LocalDate, CalendarRecordUiModel>
        get() = (content as? CalendarRecordsUiContent.Content)?.recordsByDate ?: emptyMap()

    fun recordOf(date: LocalDate): CalendarRecordUiModel? = recordsByDate[date]
}
