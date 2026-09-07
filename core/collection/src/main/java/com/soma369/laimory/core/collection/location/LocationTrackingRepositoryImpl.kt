package com.soma369.laimory.core.collection.location

import android.content.Context
import android.content.Intent
import com.soma369.laimory.core.domain.model.collection.LocationTrackingStatus
import com.soma369.laimory.core.domain.repository.LocationTrackingRepository
import com.soma369.laimory.core.util.permission.LocationPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * 위치 수집 의사를 영속([LocationTrackingPreferences])하고, 수집 FGS([LocationCollectionService])를 구동한다.
 *
 * 실제 수집은 백그라운드 지속을 위해 Foreground Service 에 위임한다(라이브 상태는 [LocationTrackingState] 공유).
 */
internal class LocationTrackingRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val preferences: LocationTrackingPreferences,
        private val trackingState: LocationTrackingState,
    ) : LocationTrackingRepository {
        override fun observeEnabled(): Flow<Boolean> = preferences.observeUserDisabled().map { disabled -> !disabled }

        override fun observeStatus(): Flow<LocationTrackingStatus?> = trackingState.status

        override suspend fun setEnabled(enabled: Boolean) {
            preferences.setUserDisabled(!enabled)
            if (enabled) start() else context.stopService(serviceIntent())
        }

        /**
         * 사용자가 끄지 않았고 권한이 있으면 수집을 켠다.
         *
         * 권한을 먼저 본다 — 없으면 FGS 승격이 실패하고, 그 실패 경로가 진행 중 세그먼트 마감까지
         * 끌고 들어간다. 켤 수 없는 것을 켜 보는 대신 다음 기회를 기다리는 편이 조용하다.
         */
        override suspend fun reconcile() {
            if (preferences.isUserDisabled()) return
            if (!canCollect()) return
            start()
        }

        private fun start() = context.startForegroundService(serviceIntent())

        private fun serviceIntent() = Intent(context, LocationCollectionService::class.java)

        private fun canCollect(): Boolean = LocationPermission.canCollect(context) && LocationPermission.hasBackground(context)
    }
