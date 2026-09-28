package com.soma369.laimory.feature.settings.state

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface InquiryUiIntent : UiIntent {
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
