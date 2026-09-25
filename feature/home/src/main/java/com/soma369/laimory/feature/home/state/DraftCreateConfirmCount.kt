package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable

/** 확인 다이얼로그의 한 칸. 단위는 [DraftConsentTypeGroup.countUnit] 을 쓴다. */
@Immutable
data class DraftCreateConfirmCount(
    val group: DraftConsentTypeGroup,
    val count: Int,
)
