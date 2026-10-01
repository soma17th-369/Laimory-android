package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.ui.base.UiSideEffect

sealed interface InquiryUiSideEffect : UiSideEffect {
    /** 사진 선택기를 연다. [maxItems] 는 남은 자리 수다. */
    data class LaunchPhotoPicker(
        val maxItems: Int,
    ) : InquiryUiSideEffect

    /** 문의를 보냈다. 방금 보낸 문의가 보이도록 문의 내역 탭으로 넘어간다. */
    data object ShowHistory : InquiryUiSideEffect

    data class ShowSnackbar(
        val message: String,
    ) : InquiryUiSideEffect
}
