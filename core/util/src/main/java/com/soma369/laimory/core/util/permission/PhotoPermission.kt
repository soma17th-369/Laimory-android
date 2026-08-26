package com.soma369.laimory.core.util.permission

import android.Manifest
import android.content.Context
import android.os.Build

/**
 * 사진 접근 권한의 Android 버전별 공통 정책.
 *
 * API 34 이상은 전체 사진과 사용자가 고른 일부 사진 접근을 **모두 허용 상태로 인정한다** —
 * 일부만 고른 사용자는 거부한 것이 아니라 범위를 정한 것이다.
 * EXIF 위치 권한은 best-effort 이므로 없어도 사진 후보 조회와 선택은 허용한다.
 */
object PhotoPermission {
    /** @param sdkInt 판정 기준 SDK. 테스트가 버전별 조합을 고정할 수 있게 주입받는다. */
    fun mediaReadPermissions(sdkInt: Int = Build.VERSION.SDK_INT): Array<String> =
        when {
            sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                )

            sdkInt >= Build.VERSION_CODES.TIRAMISU ->
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)

            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    /** 런타임 요청에 전달할 미디어 읽기 및 EXIF 위치 권한. */
    fun required(sdkInt: Int = Build.VERSION.SDK_INT): Array<String> =
        if (sdkInt >= Build.VERSION_CODES.Q) {
            mediaReadPermissions(sdkInt) + Manifest.permission.ACCESS_MEDIA_LOCATION
        } else {
            mediaReadPermissions(sdkInt)
        }

    /** 현재 전체 또는 제한된 사진 접근이 가능한지 확인한다. */
    fun canRead(context: Context): Boolean = mediaReadPermissions().any { context.isGranted(it) }

    /** API 34 이상에서 전체 권한 없이 사용자가 고른 일부 사진만 접근 가능한지 확인한다. */
    fun isLimited(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            !context.isGranted(Manifest.permission.READ_MEDIA_IMAGES) &&
            context.isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

    /** 다중 권한 요청 결과에서 전체 또는 제한된 사진 접근이 허용됐는지 확인한다. */
    fun canRead(
        grantResult: Map<String, Boolean>,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Boolean = mediaReadPermissions(sdkInt).any { grantResult[it] == true }

    /** API 34 이상 권한 요청 결과가 일부 사진 접근인지 확인한다. */
    fun isLimited(
        grantResult: Map<String, Boolean>,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Boolean =
        sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            grantResult[Manifest.permission.READ_MEDIA_IMAGES] != true &&
            grantResult[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true
}
