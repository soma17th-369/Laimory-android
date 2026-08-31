package com.soma369.laimory.core.domain.coordinator

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 서버가 약관 미동의로 요청을 거절했다는 신호.
 *
 * 거절은 인증 API 전반에서, 그것도 여러 요청에서 동시에 날 수 있다. 화면마다 받아 처리하면
 * 빠지는 곳이 생기므로 통신 경계에서 한 번만 알리고 [TermsAgreementCoordinator] 가 받아
 * 재판정한다.
 *
 * 신호를 여기에 따로 둔 것은 **의존 고리를 끊기 위해서**다. 통신 계층이 coordinator 를 직접
 * 알면 coordinator → 저장소 → 통신 계층 → coordinator 로 순환한다. 이 클래스는 아무것도
 * 의존하지 않는다.
 *
 * 버퍼는 1이고 넘치면 오래된 것을 버린다 — 여러 번 거절당해도 해야 할 일은 재판정 한 번이다.
 */
@Singleton
class TermsGateSignal
    @Inject
    constructor() {
        private val mutableEvents =
            MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

        val events: SharedFlow<Unit> = mutableEvents.asSharedFlow()

        fun notifyAgreementRequired() {
            mutableEvents.tryEmit(Unit)
        }
    }
