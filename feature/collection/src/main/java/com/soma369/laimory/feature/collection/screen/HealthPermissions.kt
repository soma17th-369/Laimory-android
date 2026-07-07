package com.soma369.laimory.feature.collection.screen

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord

/**
 * 건강 수집 권한(Health Connect) 판정.
 *
 * Health Connect 는 일반 런타임 권한이 아니라 자체 권한 모델을 쓴다 — 요청은
 * `PermissionController.createRequestPermissionResultContract()` 로 하고 [required] 를 넘긴다.
 * 권한 UX 의 최종 형태는 이번 Task 범위 밖이며, 여기서는 수집 트리거를 위한 최소 판정만 한다.
 */
internal object HealthPermissions {
    /** 걸음수·수면 읽기 권한. 권한 요청/보유 판정의 기준. */
    val required: Set<String> =
        setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
        )

    /** 기기에서 Health Connect 를 사용할 수 있는지(설치/업데이트 상태). 없으면 연동 불가. */
    fun isAvailable(context: Context): Boolean = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
}
