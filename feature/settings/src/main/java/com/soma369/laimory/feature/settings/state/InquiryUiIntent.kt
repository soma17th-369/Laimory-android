package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface InquiryUiIntent : UiIntent {
    /**
     * 화면에 새로 들어왔다. 지난번 입력을 비운다.
     *
     * ViewModel 이 Activity 수명이라 그대로 두면 지난번에 쓰다 만 내용이나 보낸 뒤 상태가 남는다.
     * 회전 같은 구성 변경으로는 보내지 않는다(화면이 저장 상태로 한 번만 보낸다).
     */
    data object Opened : InquiryUiIntent

    data class EmailChanged(
        val email: String,
    ) : InquiryUiIntent

    /** 2,000자를 넘는 부분은 받지 않는다. */
    data class BodyChanged(
        val body: String,
    ) : InquiryUiIntent

    data object AddAttachmentClicked : InquiryUiIntent

    /** 사진 선택기가 돌려준 Uri. 남은 자리보다 많거나 이미 고른 사진이면 거른다. */
    data class AttachmentsPicked(
        val uris: List<String>,
    ) : InquiryUiIntent

    data class AttachmentRemoved(
        val uri: String,
    ) : InquiryUiIntent

    data object SubmitClicked : InquiryUiIntent

    /** 앱바 뒤로와 시스템 뒤로. 보내는 중이면 무시하고, 입력이 있으면 확인을 받는다. */
    data object BackPressed : InquiryUiIntent
}
