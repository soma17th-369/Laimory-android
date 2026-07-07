package com.soma369.laimory.feature.collection.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

/**
 * 위치 수집 권한(ACCESS_FINE/COARSE_LOCATION) 판정.
 *
 * Phase 1 은 foreground 수집이라 background 권한은 요청하지 않는다. 권한 UX 의 최종 형태는 범위 밖이며,
 * 여기서는 추적 시작을 위한 최소 판정만 한다.
 */
internal object LocationPermission {
    /** 런타임 요청에 넘길 권한 목록. */
    fun required(): Array<String> =
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

    /** 현재 위치 수집이 가능한 권한 상태인지(정밀 또는 대략 하나라도 허용). */
    fun canCollect(context: Context): Boolean =
        context.isGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
            context.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)

    /** 요청 결과 맵에서 수집 가능 여부를 판정한다. */
    fun canCollect(grantResult: Map<String, Boolean>): Boolean =
        grantResult[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grantResult[Manifest.permission.ACCESS_COARSE_LOCATION] == true

    private fun Context.isGranted(permission: String): Boolean = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}
