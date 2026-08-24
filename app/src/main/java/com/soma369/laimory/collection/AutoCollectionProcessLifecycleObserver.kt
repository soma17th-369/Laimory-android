package com.soma369.laimory.collection

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.soma369.laimory.core.domain.coordinator.AutoCollectionCoordinator
import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.collection.AutoCollectionOutcome
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.usecase.auth.ObserveAuthSessionUseCase
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱이 전경으로 올라올 때 일정·건강 자동 수집을 시작한다.
 *
 * `DraftTaskProcessLifecycleObserver` 와 자리는 같지만 책임이 달라 분리한다 — 초안 작업 추적과
 * 수집은 실패해도 서로를 막지 않아야 하고, 한쪽 수정이 다른 쪽 수명주기를 건드리면 안 된다.
 *
 * 전경 진입마다 부르되 중복 실행은 조율자의 유형별 최신성 창이 막는다. 여기서 "처음 한 번" 을
 * 따로 기억하지 않는 이유는, 그 상태를 두면 프로세스가 살아 있는 동안 다시 수집하지 않게 되기
 * 때문이다.
 *
 * 인증되지 않은 상태에서는 수집하지 않고, 세션이 끊기면 최신성 상태를 버려 다음 계정이 이전
 * 계정의 "최근 수집함" 을 물려받지 않게 한다.
 */
@Singleton
class AutoCollectionProcessLifecycleObserver
    @Inject
    constructor(
        private val coordinator: AutoCollectionCoordinator,
        private val observeAuthSessionUseCase: ObserveAuthSessionUseCase,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    ) : DefaultLifecycleObserver {
        private var lifecycleJob: Job? = null

        override fun onStart(owner: LifecycleOwner) {
            lifecycleJob?.cancel()
            lifecycleJob =
                applicationScope.launch {
                    observeAuthSessionUseCase().collect { state ->
                        when (state) {
                            AuthSessionState.Authenticated -> collectInBackground()
                            AuthSessionState.Unauthenticated -> coordinator.discard()
                            AuthSessionState.Loading -> Unit
                        }
                    }
                }
        }

        override fun onStop(owner: LifecycleOwner) {
            lifecycleJob?.cancel()
        }

        /**
         * 화면을 막지 않는 best-effort 실행.
         *
         * 조율자가 값으로 돌려준 결과를 여기서만 기록한다 — 도메인은 플랫폼 로거를 모른다.
         * 유형과 건수만 남기고 일정 제목·건강 값은 남기지 않는다.
         */
        private fun collectInBackground() {
            applicationScope.launch {
                coordinator.refresh().outcomes.forEach { (type, outcome) -> log(type, outcome) }
            }
        }

        private fun log(
            type: ItemType,
            outcome: AutoCollectionOutcome,
        ) {
            when (outcome) {
                is AutoCollectionOutcome.Collected ->
                    Logger.d(LogDomain.COLLECTION, "자동 수집 완료: $type ${outcome.savedCount}건")

                AutoCollectionOutcome.PermissionDenied ->
                    Logger.d(LogDomain.COLLECTION, "자동 수집 건너뜀(권한 없음): $type")

                AutoCollectionOutcome.Unavailable ->
                    Logger.d(LogDomain.COLLECTION, "자동 수집 건너뜀(플랫폼 미지원): $type")

                AutoCollectionOutcome.Failed ->
                    Logger.w(LogDomain.COLLECTION, "자동 수집 실패: $type")
            }
        }
    }
