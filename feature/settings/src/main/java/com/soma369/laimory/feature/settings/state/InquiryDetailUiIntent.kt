package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface InquiryDetailUiIntent : UiIntent {
    /** 이 문의를 연다. 경로 인자가 깨졌으면 `null` 이고, 찾을 수 없는 문의로 보여 준다. */
    data class Load(
        val inquiryId: Long?,
    ) : InquiryDetailUiIntent

    data object Retry : InquiryDetailUiIntent

    data object NavigateBack : InquiryDetailUiIntent
}
