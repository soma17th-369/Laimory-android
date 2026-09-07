package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.repository.LocationTrackingRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 위치 수집 실행 상태를 지금 조건에 맞춘다.
 *
 * 권한을 나중에 허용했거나, 온보딩을 보지 않는 계정이거나, 실패로 한 번 멈췄던 경우를 모두
 * 이 한 곳이 받는다. 부르는 쪽은 "켜라" 가 아니라 "맞춰라" 라고만 말한다 — 사용자가 껐는지와
 * 권한이 있는지는 리포지토리가 안다.
 */
@Singleton
class ReconcileLocationTrackingUseCase
    @Inject
    constructor(
        private val repository: LocationTrackingRepository,
    ) {
        suspend operator fun invoke() = repository.reconcile()
    }
