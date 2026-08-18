package com.soma369.laimory.feature.timeline.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.feature.timeline.model.CalendarRecordUiModel
import java.time.LocalDate

/**
 * 월 한 칸의 조회 상태.
 *
 * 화면 전체가 아니라 월마다 이 상태를 갖는다 — pager 를 언마운트하지 않고 페이지 안에서 로딩·실패를
 * 표현해야 월을 넘기는 도중에 격자와 요일 헤더가 사라지지 않는다.
 */
@Immutable
sealed interface MonthlyRecordsUiContent {
    /** 아직 내용이 없고 조회 중이다. */
    data object Loading : MonthlyRecordsUiContent

    /** 내용이 없는 상태로 조회에 실패했다. 페이지 안에서 다시 시도할 수 있다. */
    data object LoadFailed : MonthlyRecordsUiContent

    /**
     * 조회한 날짜별 기록. 기록이 없는 월도 빈 맵을 가진 정상 상태다.
     *
     * [isStale] 은 복귀 등으로 무효화됐지만 아직 다시 검증하지 못했다는 뜻이다. 내용을 비우지 않으므로
     * 화면은 그대로 두고, 그 월이 다시 필요해질 때 재조회한다.
     */
    @Immutable
    data class Records(
        val recordsByDate: Map<LocalDate, CalendarRecordUiModel>,
        val isStale: Boolean = false,
    ) : MonthlyRecordsUiContent
}
