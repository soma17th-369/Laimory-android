package com.soma369.laimory.feature.timeline.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.ui.base.UiState
import java.time.LocalDate

@Immutable
data class TimelineUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val summaryRows: List<SourceSummaryRow> = emptyList(),
    val showDatePicker: Boolean = false,
    /** 확인 다이얼로그를 띄운 업로드 대상. null 이면 다이얼로그를 닫는다. */
    val pendingUpload: UploadTarget? = null,
) : UiState

/** 요약 패널 한 줄 — 카테고리 라벨과 그날 창 안 아이템 개수. */
@Immutable
data class SourceSummaryRow(
    val label: String,
    val count: Int,
)
