package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface InquiriesUiIntent : UiIntent {
    /**
     * 문의 화면에 새로 들어왔다. 보관된 목록을 비운다.
     *
     * ViewModel 이 Activity 수명이라, 로그아웃 뒤 다른 계정으로 들어와도 이전 계정의 문의가 남아 있다.
     * 계정이 바뀌려면 화면을 반드시 새로 열어야 하므로 여기서 비우면 된다(회전으로는 보내지 않는다).
     */
    data object Opened : InquiriesUiIntent

    /**
     * 진입·복귀(ON_RESUME) 시 목록을 다시 받는다. 처리 상태는 관리자가 바꾸는 값이라 캐시하지 않는다.
     * 이미 목록을 보여 주는 중이면 그대로 둔 채 갱신한다(깜빡임 방지).
     */
    data object Sync : InquiriesUiIntent

    data class InquiryClicked(
        val inquiryId: Long,
    ) : InquiriesUiIntent
}
