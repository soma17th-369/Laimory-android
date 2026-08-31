package com.soma369.laimory.core.domain.usecase.terms

import com.soma369.laimory.core.domain.model.terms.TermLinks
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.domain.repository.TermsRepository
import javax.inject.Inject

/**
 * 이용약관과 개인정보 처리방침의 원문 주소를 가져온다. **열람 전용**이라 동의 판정과 무관하다.
 *
 * 인증이 필요 없다 — 로그인 화면도 같은 길로 연다. 처리방침은 로그인 전에도 볼 수 있어야 한다.
 *
 * 실패를 예외로 올리지 않고 빈 값으로 흘린다. 약관 조회가 안 된다고 로그인 버튼을 막을 이유가
 * 없고, 화면은 링크만 비활성으로 두면 된다.
 */
class GetPublicTermLinksUseCase
    @Inject
    constructor(
        private val repository: TermsRepository,
    ) {
        suspend operator fun invoke(): TermLinks {
            val documents =
                runCatching { repository.getPublishedTerms(REQUESTED) }
                    .getOrDefault(emptyList())
                    .associateBy { it.termType }
            return TermLinks(
                termsOfService = documents[TermType.TERMS_OF_SERVICE],
                privacyPolicy = documents[TermType.PRIVACY_POLICY],
            )
        }

        private companion object {
            val REQUESTED = listOf(TermType.TERMS_OF_SERVICE, TermType.PRIVACY_POLICY)
        }
    }
