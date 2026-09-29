package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface InquiriesUiIntent : UiIntent {
    /**
     * 진입·복귀(ON_RESUME) 시 목록을 다시 받는다. 처리 상태는 관리자가 바꾸는 값이라 캐시하지 않는다.
     * 이미 목록을 보여 주는 중이면 그대로 둔 채 갱신한다(깜빡임 방지).
     */
    data object Sync : InquiriesUiIntent

    data class InquiryClicked(
        val inquiryId: Long,
    ) : InquiriesUiIntent

    data object NavigateBack : InquiriesUiIntent
}
