package com.soma369.laimory.core.collection.location

import android.content.Context
import android.content.Intent
import com.soma369.laimory.core.domain.model.collection.LocationTrackingStatus
import com.soma369.laimory.core.domain.repository.LocationTrackingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 위치 추적 토글을 영속([LocationTrackingPreferences])하고, 수집 FGS([LocationCollectionService])를 구동한다.
 *
 * Phase 2 는 백그라운드 지속을 위해 실제 수집을 Foreground Service 에 위임한다(라이브 상태는
 * [LocationTrackingState] 공유). 토글 의도는 영속되어 부팅([LocationBootReceiver])·프로세스 재시작에도 복원된다.
 */
internal class LocationTrackingRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val preferences: LocationTrackingPreferences,
        private val trackingState: LocationTrackingState,
    ) : LocationTrackingRepository {
        override fun observeEnabled(): Flow<Boolean> = preferences.observeEnabled()

        override fun observeStatus(): Flow<LocationTrackingStatus?> = trackingState.status

        override suspend fun setEnabled(enabled: Boolean) {
            preferences.setEnabled(enabled)
            val intent = Intent(context, LocationCollectionService::class.java)
            if (enabled) {
                context.startForegroundService(intent)
            } else {
                context.stopService(intent)
            }
        }
    }
