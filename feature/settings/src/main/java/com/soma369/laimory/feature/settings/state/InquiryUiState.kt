package com.soma369.laimory.feature.settings.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.inquiry.InquiryInputRules
import com.soma369.laimory.core.ui.base.UiState

/**
 * @param attachmentUris 고른 사진의 content Uri, 고른 순서대로.
 * @param emailError 보내기를 눌렀을 때 주소가 규칙에 맞지 않으면 채운다. 입력 중에는 띄우지 않는다 —
 *   `user@` 까지 친 사람에게 틀렸다고 말하는 셈이다. 주소를 고치기 시작하면 지운다.
 */
@Immutable
data class InquiryUiState(
    val email: String = "",
    val title: String = "",
    val description: String = "",
    val attachmentUris: List<String> = emptyList(),
    val emailError: String? = null,
    val isSubmitting: Boolean = false,
) : UiState {
    /** 주소 형식은 누를 때 본다. 비어 있지만 않으면 누를 수 있게 둬야 무엇이 틀렸는지 알려 줄 수 있다. */
    val canSubmit: Boolean
        get() =
            !isSubmitting &&
                email.isNotBlank() &&
                InquiryInputRules.isValidTitle(title) &&
                InquiryInputRules.isValidDescription(description)

    val remainingAttachmentSlots: Int
        get() = (InquiryInputRules.MAX_ATTACHMENTS - attachmentUris.size).coerceAtLeast(0)

    /** 나갈 때 확인을 받을지. 무엇이든 입력했거나 골랐으면 잃을 것이 있다. */
    val hasInput: Boolean
        get() = email.isNotBlank() || title.isNotBlank() || description.isNotBlank() || attachmentUris.isNotEmpty()
}
