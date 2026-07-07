package com.soma369.laimory.feature.collection.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager

/**
 * 일정 수집 권한(READ_CALENDAR) 판정.
 *
 * 권한 UX(카피/재요청 정책)의 최종 형태는 이번 Task 범위 밖이며, 여기서는 수집 트리거를 위한 최소 판정만 한다.
 */
internal object CalendarPermission {
    /** 런타임 요청에 넘길 권한 목록. */
    fun required(): Array<String> = arrayOf(Manifest.permission.READ_CALENDAR)

    /** 현재 일정 수집이 가능한 권한 상태인지. */
    fun canCollect(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** 요청 결과 맵에서 수집 가능 여부를 판정한다. */
    fun canCollect(grantResult: Map<String, Boolean>): Boolean = grantResult[Manifest.permission.READ_CALENDAR] == true
}
