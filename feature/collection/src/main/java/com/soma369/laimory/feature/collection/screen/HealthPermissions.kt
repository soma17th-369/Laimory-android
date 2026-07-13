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
    /** 걸음수·수면 읽기 권한. 수집 요청/보유 판정의 기준. */
    val required: Set<String> =
        setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
        )

    /**
     * 수면 프로듀서(HC 쓰기) 권한 — 정합성 조회(읽기) + 기록(쓰기) + 백그라운드 읽기.
     *
     * Sleep API 감지분을 HC 에 기록하려면 쓰기 권한이, 그 밤 외부 기록 존재 확인(dedup)에는 읽기 권한이 필요하다.
     * 자동 감지는 백그라운드 receiver 에서 dedup read 를 하므로 [PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND] 도 함께 요청한다.
     * 수면 기능 UI 의 권한 요청 흐름에서 이 세트를 launch 한다. 수집 게이트인 [required] 와 분리해
     * 수집이 쓰기 권한에 묶이지 않게 한다.
     */
    val sleepProducer: Set<String> =
        setOf(
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getWritePermission(SleepSessionRecord::class),
            HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND,
        )

    /** 기기에서 Health Connect 를 사용할 수 있는지(설치/업데이트 상태). 없으면 연동 불가. */
    fun isAvailable(context: Context): Boolean = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
}
