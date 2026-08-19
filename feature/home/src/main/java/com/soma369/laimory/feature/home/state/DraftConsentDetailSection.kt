package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable

/**
 * 유형 상세의 항목 묶음. 위치 유형은 체류/이동 섹션으로 구분되고, 나머지는 단일 섹션(제목 없음)이다.
 *
 * [iconPackageName]은 알림 유형에서만 채우는 앱 아이콘 조회 식별자다. 표시명([title])은
 * 중복되거나 바뀔 수 있어 아이콘 조회와 섹션 구분의 기준으로 쓰지 않는다.
 */
@Immutable
data class DraftConsentDetailSection(
    val title: String?,
    val items: List<DraftConsentDetailItem>,
    val iconPackageName: String? = null,
)
