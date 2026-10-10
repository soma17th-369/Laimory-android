package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.credit.CreditCosts

/**
 * 크레딧 잔액과 비용 조회. 앱은 읽기만 한다 — 지급(가입 시 한 번)·차감(타임라인 결과 저장 시)은 서버가 한다.
 */
interface CreditRepository {
    /**
     * 로그인 계정의 남은 크레딧.
     *
     * 서버는 크레딧 행이 없으면 기본값 대신 500 을 낸다. 실패를 0 으로 바꾸지 말 것 — 남은 게 없다는 뜻이 된다.
     */
    suspend fun getRemainingCredits(): Int

    /** 작업별 비용. 인증이 필요 없다. */
    suspend fun getCosts(): CreditCosts
}
