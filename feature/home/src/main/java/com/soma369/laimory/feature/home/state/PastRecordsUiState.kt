package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.ui.base.UiState
import com.soma369.laimory.feature.home.model.PastRecordUiModel
import java.time.YearMonth

/** 지난 기록 전용 화면의 상태. */
@Immutable
data class PastRecordsUiState(
    val content: PastRecordsContent = PastRecordsContent.Loading,
) : UiState

/** 목록 조회 결과. 홈에 있던 세 상태를 그대로 옮겼다. */
@Immutable
sealed interface PastRecordsContent {
    data object Loading : PastRecordsContent

    /** 서버 전체 조회 결과가 빈 목록인 경우. */
    data object Empty : PastRecordsContent

    /** 네트워크 오류 등으로 목록 조회에 실패한 경우. 다시 시도할 수 있다. */
    data object LoadFailed : PastRecordsContent

    /** 월별로 묶은 기록. 최신 달이 위이고, 달 안에서는 서버 정렬을 보존한다. */
    data class Groups(
        val months: List<PastRecordMonthGroup>,
    ) : PastRecordsContent
}

/**
 * 서버 정렬을 보존한 채 달로 나눈다.
 *
 * 달 안의 순서는 건드리지 않는다 — 서버가 정한 순서가 목록의 정본이다. 달끼리만 최신이 위로
 * 오도록 세운다.
 */
internal fun List<PastRecordUiModel>.toMonthGroups(): List<PastRecordMonthGroup> =
    groupBy { YearMonth.from(it.recordDate) }
        .map { (month, records) -> PastRecordMonthGroup(yearMonth = month, records = records) }
        .sortedByDescending(PastRecordMonthGroup::yearMonth)
