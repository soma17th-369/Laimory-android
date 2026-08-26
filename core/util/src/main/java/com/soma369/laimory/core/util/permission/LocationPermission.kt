package com.soma369.laimory.core.util.permission

import android.Manifest
import android.content.Context
import android.os.Build

/**
 * 위치 수집 권한 판정.
 *
 * 위치 수집은 `location` 타입 Foreground Service 로 백그라운드에서도 이어진다. Android 14+ 는 FGS
 * 시작 시점에 위치가 "eligible" 해야 하므로(while-in-use 만으로는 거부됨)
 * **`ACCESS_BACKGROUND_LOCATION`("항상 허용")이 필요**하다.
 *
 * 그래서 전경 위치 → 백그라운드 위치 순서로 나눠 요청한다. 둘을 한 번에 요청할 수 없다 —
 * 시스템이 전경 허용 뒤에만 백그라운드 요청을 받는다.
 */
object LocationPermission {
    /**
     * 1단계 요청 목록(전경 위치 + 알림 + 이동수단 인식). 백그라운드 위치는 [background] 로 따로 요청한다.
     *
     * @param sdkInt 판정 기준 SDK. 테스트가 버전별 조합을 고정할 수 있게 주입받는다.
     */
    fun required(sdkInt: Int = Build.VERSION.SDK_INT): Array<String> =
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            addAll(AppNotificationPermission.required(sdkInt))
            // 이동수단 인식(Q+). 거부돼도 수집은 진행되며 속도 추론으로 폴백한다.
            if (sdkInt >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }.toTypedArray()

    /** 2단계 요청 권한(백그라운드 위치, "항상 허용"). */
    fun background(): String = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    /** 1단계 권한 중 하나라도 미허용이면 true — 위치가 이미 허용돼도 알림·활동 권한을 놓치지 않게 한다. */
    fun needsForegroundRequest(context: Context): Boolean = required().any { !context.isGranted(it) }

    /** 전경 위치 수집 가능 여부(정밀 또는 대략 하나라도 허용). */
    fun canCollect(context: Context): Boolean =
        context.isGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
            context.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)

    /** 요청 결과 맵에서 전경 위치 수집 가능 여부를 판정한다. */
    fun canCollect(grantResult: Map<String, Boolean>): Boolean =
        grantResult[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grantResult[Manifest.permission.ACCESS_COARSE_LOCATION] == true

    /** 백그라운드 위치 허용 여부(Q 미만은 전경 권한으로 커버되므로 항상 true). */
    fun hasBackground(
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Boolean = sdkInt < Build.VERSION_CODES.Q || context.isGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    /** 이동수단 인식 허용 여부(Q 미만은 요청 대상이 아니므로 항상 true). */
    fun hasActivityRecognition(
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Boolean = sdkInt < Build.VERSION_CODES.Q || context.isGranted(Manifest.permission.ACTIVITY_RECOGNITION)
}
