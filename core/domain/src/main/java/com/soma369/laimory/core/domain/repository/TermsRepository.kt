package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.terms.TermAgreement
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermType

/** 약관 catalog 조회와 동의 등록. 동의 이력의 정본은 서버이며 앱은 어디에도 영속하지 않는다. */
interface TermsRepository {
    /**
     * 요청한 종류의 **현재 유효** 약관을 가져온다.
     *
     * 아직 활성화되지 않은 종류는 목록에서 빠지고, 전부 없으면 빈 목록이다. 오류가 아니다.
     */
    suspend fun getCurrentTerms(types: List<TermType>): List<TermDocument>

    /**
     * **열람 링크 전용** 조회. 동의 판정에 쓰지 않는다.
     *
     * 이 환경 catalog 가 비어 있으면 게시된 정본에서 주소만 가져온다 — 원문은 환경과 무관한
     * 하나의 공개 문서이고, 처리방침은 어느 빌드에서든 볼 수 있어야 한다. 판정·등록이 이 길을
     * 쓰면 다른 환경의 버전으로 동의를 보내게 되어 전부 거절된다.
     */
    suspend fun getPublishedTerms(types: List<TermType>): List<TermDocument>

    /** 이 계정의 동의 이력 전부. 없으면 빈 목록이다. */
    suspend fun getMyAgreements(): List<TermAgreement>

    /**
     * 동의를 일괄 등록한다. 조회 응답의 `(종류, 버전)` 을 그대로 돌려보내는 계약이다.
     *
     * all-or-nothing 이라 하나라도 현재 유효 버전이 아니면 아무것도 기록되지 않고
     * [com.soma369.laimory.core.domain.exception.StaleTermVersionException] 이 난다.
     * 같은 버전을 다시 보내는 것은 성공이며 최초 수락 시각을 덮어쓰지 않는다.
     */
    suspend fun agree(documents: List<TermDocument>)
}
