package com.soma369.laimory.feature.timeline.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.feature.timeline.model.CalendarRecordUiModel
import java.time.LocalDate

/** 캘린더가 표시하는 서버 기록 목록의 조회 상태. */
@Immutable
sealed interface CalendarRecordsUiContent {
    data object Loading : CalendarRecordsUiContent

    /**
     * 서버 전체 조회 결과가 빈 목록인 경우.
     *
     * 표시 중인 월에만 기록이 없는 경우와 다르다 — 그 경우는 정상 [Content] 의 빈 격자로 표시한다.
     */
    data object Empty : CalendarRecordsUiContent

    /** 네트워크 오류 등으로 전체 조회에 실패한 경우. 다시 시도할 수 있다. */
    data object LoadFailed : CalendarRecordsUiContent

    /** 날짜별 대표 기록. 월 전환은 이 맵을 다시 필터링할 뿐 서버를 다시 부르지 않는다. */
    @Immutable
    data class Content(
        val recordsByDate: Map<LocalDate, CalendarRecordUiModel>,
    ) : CalendarRecordsUiContent
}
