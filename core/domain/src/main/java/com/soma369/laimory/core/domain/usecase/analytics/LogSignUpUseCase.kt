package com.soma369.laimory.core.domain.usecase.analytics

import com.soma369.laimory.core.domain.helper.AnalyticsHelper
import com.soma369.laimory.core.domain.model.analytics.AnalyticsDedupeKeys
import com.soma369.laimory.core.domain.model.analytics.AnalyticsEvent
import com.soma369.laimory.core.domain.model.auth.SocialLoginProvider
import com.soma369.laimory.core.domain.repository.OnboardingRepository
import javax.inject.Inject

/**
 * 로그인 직후 새 계정이면 가입을 기록한다. 설치당 한 번.
 *
 * 서버가 신규 여부를 따로 주지 않아, **온보딩을 마치지 않은 계정**을 새 계정으로 추정한다. 새 계정은 언제나
 * 온보딩 전이다. 온보딩을 끝내지 않은 채 재설치한 기존 계정이 섞이는 것은 감수한다.
 *
 * 서버에 직접 묻는다 — 캐시로 떨어진 값은 조회가 실패해 "아직 안 함" 으로 본 것일 수 있어 가입으로 세면
 * 오프라인 재로그인이 가입이 된다. 조회가 실패하면 기록하지 않는다.
 */
class LogSignUpUseCase
    @Inject
    constructor(
        private val onboardingRepository: OnboardingRepository,
        private val analyticsHelper: AnalyticsHelper,
    ) {
        suspend operator fun invoke(provider: SocialLoginProvider) {
            // 이 설치에서 끝냈지만 아직 서버에 못 올린 온보딩이면 서버는 `false` 를 준다. 새 계정이 아니다.
            if (onboardingRepository.isCompletionPending()) return
            val isCompleted = onboardingRepository.fetchCompletion().getOrNull() ?: return
            if (isCompleted) return
            analyticsHelper.logOnce(AnalyticsDedupeKeys.SIGN_UP, AnalyticsEvent.SignUp(provider))
        }
    }
