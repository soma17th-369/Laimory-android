package com.soma369.laimory.core.domain.model.collection

/**
 * 리스너 경계에서 Android 알림 객체를 변환한 구조 신호.
 *
 * 프레임워크 타입과 상수를 정책 안으로 들이지 않기 위한 값 객체다. 각 신호는 리스너에서
 * 판정해 boolean 으로만 넘긴다.
 *
 * 로컬 저장분에는 남기지 않으므로 조회 경계에서 다시 복원할 수 없다 —
 * 그 경우 [UNAVAILABLE]을 넘기며, 구조 신호에 기대는 규칙은 적용되지 않는다.
 */
data class NotificationSignals(
    /** 개인·그룹 대화 알림인지 여부(`CATEGORY_MESSAGE` 또는 MessagingStyle). */
    val isMessage: Boolean = false,
    /** 앱이 스스로 광고로 선언한 알림(`CATEGORY_PROMO`). */
    val isPromotion: Boolean = false,
    /** 배달 추적·재생·내비게이션처럼 진행 중 상태를 계속 보여주는 알림(`FLAG_ONGOING_EVENT`). */
    val isOngoing: Boolean = false,
    /** 개별 알림과 내용이 겹치는 묶음 요약(`FLAG_GROUP_SUMMARY`). */
    val isGroupSummary: Boolean = false,
    /** 진행률을 갱신하는 알림(`EXTRA_PROGRESS` 계열). */
    val hasProgress: Boolean = false,
) {
    /**
     * 생활 이벤트로 보기 어려운 알림.
     *
     * 텍스트 광고 키워드는 정상 이벤트와 같은 단어를 써서 오탐이 크므로 1차에서는 쓰지 않고,
     * 앱이 구조로 드러낸 신호만 사용한다.
     */
    val isNonEvent: Boolean get() = isOngoing || hasProgress || isGroupSummary || isPromotion

    companion object {
        /** 구조 신호를 알 수 없는 경계(로컬 저장분 재적용)에서 쓴다. 텍스트 규칙만 적용된다. */
        val UNAVAILABLE = NotificationSignals()
    }
}
