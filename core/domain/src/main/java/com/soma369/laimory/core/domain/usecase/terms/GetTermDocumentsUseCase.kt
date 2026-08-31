package com.soma369.laimory.core.domain.usecase.terms

import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.domain.repository.TermsRepository
import javax.inject.Inject

/**
 * 원문을 열기 위한 약관 주소를 가져온다. 동의 판정과 무관한 **열람 전용** 경로다.
 *
 * 인증이 필요 없다 — 로그인 화면도 같은 길로 약관을 연다. 실패는 예외로 던지지 않고 빈 목록으로
 * 흘린다. 약관 조회가 안 된다고 로그인 버튼을 막을 이유가 없고, 화면은 링크만 비활성으로 두면 된다.
 */
class GetTermDocumentsUseCase
    @Inject
    constructor(
        private val repository: TermsRepository,
    ) {
        suspend operator fun invoke(types: List<TermType>): List<TermDocument> =
            runCatching { repository.getCurrentTerms(types) }.getOrDefault(emptyList())
    }
