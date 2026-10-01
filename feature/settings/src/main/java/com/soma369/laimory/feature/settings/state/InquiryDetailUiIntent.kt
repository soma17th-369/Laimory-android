package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface InquiryDetailUiIntent : UiIntent {
    /**
     * 상세 화면에 새로 들어왔다. 같은 문의라도 보관된 내용을 비우고 새로 받는다.
     *
     * ViewModel 이 Activity 수명이라, 로그아웃 뒤 다른 계정으로 들어와도 이전 계정의 내용이 남아 있다.
     */
    data class Opened(
        val inquiryId: Long?,
    ) : InquiryDetailUiIntent

    /** 이 문의를 연다. 경로 인자가 깨졌으면 `null` 이고, 찾을 수 없는 문의로 보여 준다. */
    data class Load(
        val inquiryId: Long?,
    ) : InquiryDetailUiIntent

    data object Retry : InquiryDetailUiIntent

    data object NavigateBack : InquiryDetailUiIntent
}
