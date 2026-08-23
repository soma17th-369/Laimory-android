package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.user.UserProfile

/** 서버 회원 계정 계약. 회원 정보 조회와 탈퇴 요청이 같은 계정 경계에 있다. */
interface UserRepository {
    /** 현재 인증 세션 계정의 회원 정보를 조회한다. */
    suspend fun getMyProfile(): UserProfile

    /**
     * 현재 인증 세션 계정의 탈퇴를 요청한다.
     *
     * 서버가 접수를 commit 하면 정상 반환한다. 물리 삭제 완료를 뜻하지 않으며, 로컬 인증 세션은
     * 이 호출이 정리하지 않는다.
     */
    suspend fun requestAccountWithdrawal()
}
