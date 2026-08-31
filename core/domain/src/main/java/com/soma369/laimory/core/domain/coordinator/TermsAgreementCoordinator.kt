package com.soma369.laimory.core.domain.coordinator

import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermStage
import com.soma369.laimory.core.domain.model.terms.TermStageRequirement
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.domain.model.terms.TermsGateState
import kotlinx.coroutines.flow.StateFlow

/**
 * 인증 세션 하나당 약관 catalog 와 동의 이력을 한 번만 읽어 앱 전체가 나눠 쓴다.
 *
 * 이력의 정본은 서버다. 앱은 세션 동안만 들고 있고 어디에도 영속하지 않는다 — 사용자가 다른
 * 기기에서 동의하거나 약관이 개정되면 저장해 둔 판정은 곧 틀린 값이 된다.
 */
interface TermsAgreementCoordinator {
    /** 이용약관 단계 판정. 앱 루트가 이 값으로 갈린다. */
    val loginGate: StateFlow<TermsGateState>

    /** 다시 판정한다. 이미 진행 중인 조회가 있으면 그것을 기다린다 — 동시에 여러 번 불러도 조회는 한 번이다. */
    fun refresh()

    /** 한 단계의 요구 상태. 조회에 실패하면 실패를 그대로 돌려준다. */
    suspend fun requirementOf(stage: TermStage): Result<TermStageRequirement>

    /**
     * 이미 읽어 둔 문서 하나를 꺼낸다. **열람 링크용**이라 동의 판정과 무관하다.
     *
     * 처리방침처럼 동의 대상이 아닌 문서도 여기서 나온다 — 판정에 필요한 종류만 읽으면 화면이
     * 링크 하나 때문에 서버를 또 부르게 된다.
     */
    suspend fun documentOf(type: TermType): TermDocument?

    /**
     * 동의를 등록한다. 성공하면 세션 판정에 즉시 반영한다.
     *
     * 개정으로 버전이 어긋나면
     * [com.soma369.laimory.core.domain.exception.StaleTermVersionException] 이 실패로 나온다.
     * 호출부는 **새 버전으로 다시 보내지 말고** 재조회해 사용자에게 다시 확인받아야 한다.
     */
    suspend fun agree(documents: List<TermDocument>): Result<Unit>
}
