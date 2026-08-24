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

    /**
     * 확인 체크박스를 켜야 primary 를 누를 수 있는 두 버튼 Dialog.
     *
     * 되돌릴 수 없는 동작에 쓴다 — 안내를 읽지 않고 습관적으로 누르는 것을 막으려고 확인 동작을
     * 한 단계 더 둔다. 체크 상태는 표시 중에만 필요한 값이라 이 요청에도 [DialogResult] 에도
     * 담기지 않는다. 체크되지 않은 채로는 primary 를 누를 수 없으므로 [DialogResult.Primary] 자체가
     * 동의를 뜻한다.
     */
    data class Consent(
        override val title: String,
        override val body: String,
        val consentLabel: String,
        val primaryLabel: String,
        val secondaryLabel: String,
        val primaryStyle: DialogActionStyle = DialogActionStyle.PRIMARY,
        override val dismissible: Boolean = true,
    ) : DialogRequest
}
