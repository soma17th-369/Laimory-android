package com.soma369.laimory.core.collection.location

import android.content.Context
import android.content.Intent
import com.soma369.laimory.core.domain.model.collection.LocationTrackingStatus
import com.soma369.laimory.core.domain.repository.LocationTrackingRepository
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
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

        /**
         * 사용자의 의사를 쓰는 **유일한 자리**다. 설정 토글도 알림의 `중지`([LocationStopReceiver])도 여기로 온다.
         *
         * 서비스가 대신 쓰지 않는다 — 중지 지시는 큐를 거쳐 늦게 처리될 수 있어, 그 사이에 사용자가
         * 다시 켜 두었으면 지난 결정이 최신 값을 덮어쓴다. 값은 조작을 받은 그 자리에서 쓴다.
         */
        override suspend fun setEnabled(enabled: Boolean) {
            preferences.setUserDisabled(!enabled)
            if (enabled) start() else stopByUser()
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

        /**
         * 시작 실패는 삼킨다.
         *
         * Android 12+ 는 앱이 전경을 벗어난 뒤의 `startForegroundService` 를 거부한다
         * (`ForegroundServiceStartNotAllowedException`). 설정에서 토글을 누른 직후 앱을 벗어나면 이
         * 호출이 백그라운드에서 처리될 수 있는데, 예외가 Intent 소비 루프까지 올라가면 그 화면의
         * 나머지 동작까지 함께 죽는다. 사용자의 의사는 이미 저장됐으므로 다음 전경 진입이 다시 켠다.
         */
        private fun start() {
            runCatching { context.startForegroundService(serviceIntent()) }
                .onFailure { e -> Logger.w(LogDomain.COLLECTION, "위치 수집 시작 실패: ${e::class.simpleName}") }
        }

        /**
         * 마감을 지시해 멈춘다. 의사는 [setEnabled] 가 이미 썼으므로 여기서는 지시만 보낸다.
         *
         * `stopService` 로 바로 끊으면 서비스가 그것을 시스템에 의한 종료로 읽어 진행 중 구간을
         * **스냅샷으로 보존한다** — 프로세스가 죽었다 살아났을 때 이어 붙이기 위한 길이다. 사용자가
         * 끈 것은 이어 붙일 것이 아니라 거기서 끝난 것이라, 마지막 샘플까지 저장하고 스냅샷을 비워야
         * 한다. 그러지 않으면 꺼 둔 시간이 이전 체류에 얹혀 하루 기록이 늘어난다.
         *
         * 서비스가 떠 있지 않아도 보낸다 — 지난 프로세스가 남긴 스냅샷을 서비스가 마감해 준다.
         */
        private fun stopByUser() {
            runCatching { context.startService(serviceIntent().setAction(LocationCollectionService.ACTION_STOP)) }
                .onFailure { e ->
                    // 백그라운드에서 꺼진 서비스를 깨우는 것은 거부될 수 있다. 그때는 확실히 멈추는 것만 한다.
                    Logger.w(LogDomain.COLLECTION, "위치 수집 중지 전달 실패: ${e::class.simpleName}")
                    runCatching { context.stopService(serviceIntent()) }
                }
        }

        private fun serviceIntent() = Intent(context, LocationCollectionService::class.java)

        private fun canCollect(): Boolean = LocationPermission.canCollect(context) && LocationPermission.hasBackground(context)
    }
