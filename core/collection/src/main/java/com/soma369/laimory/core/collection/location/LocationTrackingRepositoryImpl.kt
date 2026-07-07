package com.soma369.laimory.core.collection.location

import com.soma369.laimory.core.domain.model.collection.LocationTrackingStatus
import com.soma369.laimory.core.domain.repository.LocationTrackingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 추적 토글을 [LocationTrackingManager] 의 세션 상태에 위임한다.
 *
 * Phase 1 은 foreground 한정이라 토글을 영속 저장하지 않는다 — 콜드 스타트마다 off 로 시작한다(재실행 시
 * 자동 재개하면 추적이 실제로는 죽어있는데 토글만 켜진 것처럼 보이는 불일치가 생기므로). 백그라운드 지속·부팅
 * 복원은 Phase 2 몫이다.
 */
internal class LocationTrackingRepositoryImpl
    @Inject
    constructor(
        private val manager: LocationTrackingManager,
    ) : LocationTrackingRepository {
        override fun observeEnabled(): Flow<Boolean> = manager.isTracking

        override fun observeStatus(): Flow<LocationTrackingStatus?> = manager.status

        override suspend fun setEnabled(enabled: Boolean) {
            if (enabled) manager.start() else manager.stop()
        }
    }
