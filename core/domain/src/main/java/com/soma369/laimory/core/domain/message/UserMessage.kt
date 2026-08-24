package com.soma369.laimory.core.domain.message

/**
 * 공통 정책성 사용자 메시지의 의미 수준 표현.
 *
 * 스낵바/다이얼로그 같은 UI 표현을 직접 말하지 않는다. presentation 구현체가 실제 UI 효과로 매핑한다.
 */
sealed interface UserMessage {
    /** 세션 만료 — 재로그인 유도. */
    data object SessionExpired : UserMessage

    /** 현재 앱 버전에서 지원하지 않는 기능. */
    data object UnsupportedFeature : UserMessage

    /** 일시적 오류 — 잠시 후 재시도. */
    data object TemporaryUnavailable : UserMessage

    /** 하루 기록 작성 완료 — 화면 pop 이후에도 Root 채널로 안내한다. */
    data object DailyRecordSaved : UserMessage

    /** 회원 탈퇴 접수 완료 — 로그인 Root 로 교체된 뒤 안내한다. */
    data object AccountWithdrawalAccepted : UserMessage

    /**
     * 회원 탈퇴 요청이 `401` 로 끝났다.
     *
     * 서버가 만료·무효 세션과 이미 탈퇴한 회원을 구분하지 않으므로 **탈퇴 완료로 안내하지 않는다.**
     * 재인증이 필요한 종료 상태로만 알린다.
     */
    data object AccountWithdrawalUnverified : UserMessage
}
