package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.ui.base.UiSideEffect

sealed interface InquiryUiSideEffect : UiSideEffect {
    /** 사진 선택기를 연다. [maxItems] 는 남은 자리 수다. */
    data class LaunchPhotoPicker(
        val maxItems: Int,
    ) : InquiryUiSideEffect

    data class ShowSnackbar(
        val message: String,
    ) : InquiryUiSideEffect
}
