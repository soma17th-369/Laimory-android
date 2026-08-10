package com.soma369.laimory.core.domain.message

/**
 * 공통 메시지형 Dialog 표시 요청.
 *
 * UI 문자열·버튼 구성은 요청한 화면이 소유하고 이 모델은 통과만 한다 — Compose 타입은 노출하지 않는다.
 * 다른 Dialog가 표시 중인 동안의 새 요청은 등록되지 않고 즉시 [DialogResult.Dismissed]로 응답된다.
 */
sealed interface DialogRequest {
    val title: String
    val body: String
    val dismissible: Boolean

    data class OneButton(
        override val title: String,
        override val body: String,
        val buttonLabel: String,
        val buttonStyle: DialogActionStyle = DialogActionStyle.PRIMARY,
        override val dismissible: Boolean = true,
    ) : DialogRequest

    data class TwoButton(
        override val title: String,
        override val body: String,
        val primaryLabel: String,
        val secondaryLabel: String,
        val primaryStyle: DialogActionStyle = DialogActionStyle.PRIMARY,
        override val dismissible: Boolean = true,
    ) : DialogRequest
}
