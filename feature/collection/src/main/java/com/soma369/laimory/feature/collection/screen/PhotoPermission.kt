package com.soma369.laimory.feature.collection.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * 사진 수집 권한의 Android 버전별 분기.
 *
 * - API 34+: `READ_MEDIA_IMAGES` 또는 부분 허용 `READ_MEDIA_VISUAL_USER_SELECTED` 중 하나면 수집 가능.
 * - API 33: `READ_MEDIA_IMAGES`.
 * - API 32 이하: `READ_EXTERNAL_STORAGE`.
 *
 * EXIF 위치를 읽으려면 API 29+ 에서 `ACCESS_MEDIA_LOCATION` 이 추가로 필요하지만, 위치는 best-effort 라
 * 이 권한이 없어도 수집(위치 null)은 진행한다 — 수집 가능 판정([canCollect])은 미디어 읽기 권한만 본다.
 *
 * 권한 UX(카피/재요청 정책)의 최종 형태는 이번 Task 범위 밖이며, 여기서는 수집 트리거를 위한 최소 판정만 한다.
 */
internal object PhotoPermission {
    /** 미디어 읽기 권한(수집 가능 여부의 기준). */
    private fun mediaReadPermissions(): Array<String> =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                )

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)

            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    /** 런타임 요청에 넘길 권한 목록. 미디어 읽기 + (API 29+) EXIF 위치용 `ACCESS_MEDIA_LOCATION`. */
    fun required(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaReadPermissions() + Manifest.permission.ACCESS_MEDIA_LOCATION
        } else {
            mediaReadPermissions()
        }

    /** 현재 사진 수집이 가능한 권한 상태인지. 부분 허용(선택한 사진만)도 수집 가능으로 본다. */
    fun canCollect(context: Context): Boolean = mediaReadPermissions().any { context.isGranted(it) }

    /** 요청 결과 맵에서 수집 가능 여부를 판정한다. 미디어 읽기가 하나라도 허용되면 수집 가능(부분 허용 포함). */
    fun canCollect(grantResult: Map<String, Boolean>): Boolean = mediaReadPermissions().any { grantResult[it] == true }

    private fun Context.isGranted(permission: String): Boolean = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}
