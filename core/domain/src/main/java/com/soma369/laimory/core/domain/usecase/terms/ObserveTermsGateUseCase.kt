package com.soma369.laimory.core.domain.usecase.terms

import com.soma369.laimory.core.domain.coordinator.TermsAgreementCoordinator
import com.soma369.laimory.core.domain.model.terms.TermsGateState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 이용약관 단계 판정을 관찰한다.
 *
 * `Unknown` 이면 아직 모른다는 뜻이라 앱 루트를 정하지 않는다 — 모르는 채로 홈을 그리면 동의가
 * 필요한 사용자에게 홈이 한 프레임 번쩍인 뒤 화면이 갈린다.
 */
@Singleton
class ObserveTermsGateUseCase
    @Inject
    constructor(
        private val coordinator: TermsAgreementCoordinator,
    ) {
        operator fun invoke(): Flow<TermsGateState> = coordinator.loginGate
    }
