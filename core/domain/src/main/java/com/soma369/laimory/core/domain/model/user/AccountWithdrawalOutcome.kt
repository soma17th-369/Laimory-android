package com.soma369.laimory.core.domain.model.user

/**
 * 회원 탈퇴 요청의 terminal 결과.
 *
 * 호출부가 `ApiException.rawCode` 를 직접 해석하지 않도록 도메인 표현으로 좁혀 둔다.
 */
sealed interface AccountWithdrawalOutcome {
    /**
     * 서버가 탈퇴를 접수했다(`202`).
     *
     * 논리 탈퇴·credential 차단·push 등록 삭제와 데이터 삭제 작업 접수가 commit 됐다는 뜻이고,
     * 물리 삭제 완료를 뜻하지 않는다.
     */
    data object Accepted : AccountWithdrawalOutcome

    /**
     * 요청의 최종 응답이 `401` 이다.
     *
     * 더 진행할 수 없는 terminal 결과지만 **탈퇴 성공으로 단정하지 않는다** — 서버가 만료·무효 세션과
     * 이미 탈퇴한 회원을 같은 `401/-2001` 로 합치기 때문이다. 응답 유실 뒤 재시도에서도 이 값이
     * 나올 수 있으므로, 화면은 탈퇴 완료 문구가 아니라 재인증이 필요한 종료 상태로 다룬다.
     */
    data object SessionUnavailable : AccountWithdrawalOutcome
}
