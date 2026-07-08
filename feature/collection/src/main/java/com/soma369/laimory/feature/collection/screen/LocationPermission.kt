package com.soma369.laimory.feature.collection.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * 위치 수집 권한 판정.
 *
 * Phase 2 는 `location` 타입 Foreground Service 로 백그라운드에서도 수집한다. Android 14+ 에서는 FGS 시작 시점에
 * 위치가 "eligible" 해야 하므로(while-in-use 만으로는 거부됨) **`ACCESS_BACKGROUND_LOCATION`("항상 허용")이 필요**하다.
 * 그래서 켤 때 전경 위치(FINE/COARSE)+알림(Android 13+ `POST_NOTIFICATIONS`)을 먼저 받고, 그다음 백그라운드 위치를
 * 단계적으로 요청한다(둘은 한 번에 요청할 수 없다).
 */
internal object LocationPermission {
    /** 1단계 요청 목록(전경 위치 + Android 13+ 알림). 백그라운드 위치는 별도 단계로 요청한다. */
    fun required(): Array<String> =
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            // 이동수단 인식(Q+). 거부돼도 수집은 진행되며 속도 추론으로 폴백한다.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }.toTypedArray()

    /** 2단계 요청 권한(백그라운드 위치, "항상 허용"). */
    fun background(): String = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    /** 전경 위치 수집 가능 여부(정밀 또는 대략 하나라도 허용). */
    fun canCollect(context: Context): Boolean =
        context.isGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
            context.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)

    /** 요청 결과 맵에서 전경 위치 수집 가능 여부를 판정한다. */
    fun canCollect(grantResult: Map<String, Boolean>): Boolean =
        grantResult[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grantResult[Manifest.permission.ACCESS_COARSE_LOCATION] == true

    /** 백그라운드 위치 허용 여부(Q 미만은 전경 권한으로 커버되므로 항상 true). */
    fun hasBackground(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            context.isGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    private fun Context.isGranted(permission: String): Boolean = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}
