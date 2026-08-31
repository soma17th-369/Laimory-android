package com.soma369.laimory.core.domain.usecase.terms

import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.domain.repository.TermsRepository
import javax.inject.Inject

/**
 * 동의 화면에 **보여 줄** 문서를 가져온다. 이 환경 catalog 가 비어 있으면 게시된 정본에서 온다.
 *
 * **등록 대상이 아니다.** 서버에 보낼 목록은 언제나
 * [com.soma369.laimory.core.domain.coordinator.TermsAgreementCoordinator.requirementOf] 가 준
 * 이 환경 문서다 — 다른 환경의 버전을 보내면 그 DB 에 없는 행이라 전부 거절되고, 재조회해도 같은
 * 값이 나와 빠져나올 수 없는 자리에 갇힌다.
 *
 * catalog 가 빈 환경에서는 서버도 그 단계를 강제하지 않으므로(fail-open) 보여 주기만 하고 아무것도
 * 기록하지 않는 것이 서버 판정과 어긋나지 않는다. 개발 catalog 에 seed 가 들어가면 이 경로는 더
 * 이상 타지 않는다.
 */
class GetDisplayTermsUseCase
    @Inject
    constructor(
        private val repository: TermsRepository,
    ) {
        suspend operator fun invoke(types: List<TermType>): List<TermDocument> =
            runCatching { repository.getPublishedTerms(types) }.getOrDefault(emptyList())
    }
