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

    /**
     * 서버가 실패 응답에 담아 준 안내 문구. 공통 정책(세션 만료·미지원·일시 오류)도 서버가 문구를 주면 이것으로 대신한다.
     *
     * 문구를 서버가 정하므로 앱 배포 없이 바뀐다. 앱이 정하는 것은 어느 실패를 공통으로 알릴지뿐이다.
     */
    data class ServerNotice(
        val text: String,
    ) : UserMessage

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

    /** 문의 접수 완료. 문의 화면을 닫은 뒤 설정 화면에서 보이도록 앱 전역으로 띄운다. */
    data object InquirySubmitted : UserMessage
}
