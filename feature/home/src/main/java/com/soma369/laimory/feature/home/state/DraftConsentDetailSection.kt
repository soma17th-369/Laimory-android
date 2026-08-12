package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable

/** 유형 상세의 항목 묶음. 위치 유형은 체류/이동 섹션으로 구분되고, 나머지는 단일 섹션(제목 없음)이다. */
@Immutable
data class DraftConsentDetailSection(
    val title: String?,
    val items: List<DraftConsentDetailItem>,
)
