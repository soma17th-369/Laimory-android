package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.feature.home.model.PastRecordUiModel
import java.time.YearMonth

/** 지난 기록 목록의 한 달 묶음. 헤더에 `2025년 5월`·`3개의 기록` 을 적는다. */
@Immutable
data class PastRecordMonthGroup(
    val yearMonth: YearMonth,
    val records: List<PastRecordUiModel>,
)
