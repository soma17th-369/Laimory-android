package com.soma369.laimory.analytics

import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.helper.AnalyticsHelper
import com.soma369.laimory.core.domain.model.auth.SignedInAccount
import com.soma369.laimory.core.domain.model.user.UserProfile
import com.soma369.laimory.core.domain.usecase.auth.ObserveSignedInAccountUseCase
import com.soma369.laimory.core.domain.usecase.user.ObserveUserProfileUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로그인 세션에 맞춰 분석의 사용자 구분(GA4 User-ID)을 걸고 푼다.
 *
 * 세션과 회원 정보를 함께 본다. 회원 정보만 보면 "로그아웃했다" 와 "아직 조회하지 못했다" 가 둘 다
 * null 이라 구분되지 않는다 — 조회 전에 풀어 버리면 SDK 가 기억하던 값이 지워져 조회가 끝날 때까지의
 * 이벤트가 사람과 끊긴다.
 */
@Singleton
class AnalyticsUserIdReporter
    @Inject
    constructor(
        private val observeSignedInAccount: ObserveSignedInAccountUseCase,
        private val observeUserProfile: ObserveUserProfileUseCase,
        private val analyticsHelper: AnalyticsHelper,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    ) {
        private var job: Job? = null

        fun start() {
            if (job?.isActive == true) return
            job =
                applicationScope.launch {
                    combine(observeSignedInAccount(), observeUserProfile(), ::analyticsUserIdChange)
                        .filterNotNull()
                        .distinctUntilChanged()
                        .collect { change ->
                            when (change) {
                                is AnalyticsUserIdChange.Assign -> analyticsHelper.setUserId(change.userId)
                                AnalyticsUserIdChange.Clear -> analyticsHelper.setUserId(null)
                            }
                        }
                }
        }
    }

/** 사용자 구분을 어떻게 바꿀지. 바꾸지 않을 때는 값 자체가 없다(`null`). */
internal sealed interface AnalyticsUserIdChange {
    data class Assign(
        val userId: Long,
    ) : AnalyticsUserIdChange

    data object Clear : AnalyticsUserIdChange
}

/**
 * 세션과 회원 정보로 사용자 구분을 정한다.
 *
 * - 세션이 없으면(로그아웃·탈퇴) 푼다.
 * - 회원 식별자를 받았으면 그 회원으로 건다.
 * - 세션은 있는데 식별자가 없으면(조회 전·조회 실패·식별자를 안 주는 서버) 그대로 둔다.
 */
internal fun analyticsUserIdChange(
    account: SignedInAccount?,
    profile: UserProfile?,
): AnalyticsUserIdChange? {
    if (account == null) return AnalyticsUserIdChange.Clear
    val userId = profile?.userId ?: return null
    return AnalyticsUserIdChange.Assign(userId)
}
