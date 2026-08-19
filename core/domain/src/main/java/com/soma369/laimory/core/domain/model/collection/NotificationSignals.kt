package com.soma369.laimory.core.domain.model.collection

/**
 * 리스너 경계에서 Android 알림 객체를 변환한 구조 신호.
 *
 * 프레임워크 타입을 정책 안으로 들이지 않기 위한 값 객체다.
 * 로컬 저장분에는 남기지 않으므로 조회 경계에서 다시 복원할 수 없다 —
 * 그 경우 [UNAVAILABLE]을 넘기며, 구조 신호에 기대는 규칙은 적용되지 않는다.
 */
data class NotificationSignals(
    /** 개인·그룹 대화 알림인지 여부(`CATEGORY_MESSAGE` 또는 MessagingStyle). */
    val isMessage: Boolean = false,
) {
    companion object {
        /** 구조 신호를 알 수 없는 경계(로컬 저장분 재적용)에서 쓴다. 텍스트 규칙만 적용된다. */
        val UNAVAILABLE = NotificationSignals()
    }
}
