package com.soma369.laimory.core.ui.permission

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import com.soma369.laimory.core.ui.BuildConfig

/**
 * Health Connect 걸음수·수면 읽기 권한.
 *
 * 다른 소스와 달리 Android 권한이 아니라 Health Connect 앱이 소유한 권한이라, 기기에 그 앱이
 * 없거나 업데이트가 필요하면 **요청 자체가 성립하지 않는다.** 그래서 허용/거부 앞에 `쓸 수
 * 있는 기기인가` 가 먼저 온다.
 */
object HealthDataSource {
    /** 수집기가 실제로 읽는 레코드와 같아야 한다 — 더 받으면 쓰지도 않는 권한을 묻는 셈이다. */
    val required: Set<String> =
        setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
        )

    /**
     * 목록에 헬스를 노출할지.
     *
     * release 는 아직 끈다 — Play Console 의 Health Connect 데이터 유형 신고와 심사가
     * 끝나야 실제로 켤 수 있어서, 그 전에 사용자에게 보이면 허용해도 아무 일도 일어나지 않는다.
     * debug 에서는 켜 두고 흐름을 확인한다.
     */
    val isEnabled: Boolean = BuildConfig.DEBUG

    fun isAvailable(context: Context): Boolean = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    /** 권한 조회는 Health Connect 클라이언트에 물어야 한다. 실패는 삼키지 않고 그대로 올린다. */
    suspend fun isGranted(context: Context): Boolean =
        HealthConnectClient
            .getOrCreate(context)
            .permissionController
            .getGrantedPermissions()
            .containsAll(required)
}
