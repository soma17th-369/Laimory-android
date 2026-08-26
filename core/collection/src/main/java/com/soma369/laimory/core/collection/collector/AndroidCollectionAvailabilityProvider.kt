package com.soma369.laimory.core.collection.collector

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import com.soma369.laimory.core.domain.provider.CollectionAvailabilityProvider
import com.soma369.laimory.core.util.permission.CalendarPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 자동 수집 가능 여부의 Android 구현.
 *
 * 판정만 하고 권한을 요청하지 않는다. 자동 수집은 사용자가 시작한 동작이 아니라 권한 요청
 * 화면을 띄우면 안 된다.
 *
 * 권한 판정은 `core:permission` 에 위임한다 — 같은 권한을 두 곳에서 판정하면 수집 실험실과
 * 자동 수집이 서로 다른 답을 낼 수 있다.
 */
@Singleton
internal class AndroidCollectionAvailabilityProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : CollectionAvailabilityProvider {
        override fun canCollectCalendar(): Boolean = CalendarPermission.isGranted(context)

        override fun isHealthConnectAvailable(): Boolean = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

        /**
         * Health Connect 권한은 시스템 권한이 아니라 클라이언트에 물어야 알 수 있다.
         *
         * **조회 자체의 실패를 권한 없음으로 축약하지 않는다.** 클라이언트 생성 실패 같은 일시적
         * 오류까지 `false` 로 만들면 자동 수집이 그것을 "권한 없음" 으로 분류한다. 권한 없음은
         * 사용자가 그렇게 설정한 정상 상태라 안내도 재시도도 하지 않는 경로여서, 일시 오류가
         * 조용히 묻힌다. 예외는 그대로 올려 조율자가 실패로 분류하게 한다.
         */
        override suspend fun canCollectHealth(): Boolean =
            HealthConnectClient
                .getOrCreate(context)
                .permissionController
                .getGrantedPermissions()
                .containsAll(REQUIRED_HEALTH_PERMISSIONS)

        private companion object {
            /** 걸음수·수면 읽기. 수집기가 실제로 읽는 레코드와 같은 집합이어야 한다. */
            val REQUIRED_HEALTH_PERMISSIONS =
                setOf(
                    HealthPermission.getReadPermission(StepsRecord::class),
                    HealthPermission.getReadPermission(SleepSessionRecord::class),
                )
        }
    }
