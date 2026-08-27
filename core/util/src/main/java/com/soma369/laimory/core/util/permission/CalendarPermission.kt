package com.soma369.laimory.core.util.permission

import android.Manifest
import android.content.Context

/** 일정 읽기 권한(`READ_CALENDAR`) 판정. */
object CalendarPermission {
    /** 런타임 요청에 넘길 권한 목록. */
    fun required(): Array<String> = arrayOf(Manifest.permission.READ_CALENDAR)

    /** 현재 일정을 읽을 수 있는 상태인지. */
    fun isGranted(context: Context): Boolean = context.isGranted(Manifest.permission.READ_CALENDAR)

    /** 요청 결과 맵에서 허용 여부를 판정한다. */
    fun isGranted(grantResult: Map<String, Boolean>): Boolean = grantResult[Manifest.permission.READ_CALENDAR] == true
}
