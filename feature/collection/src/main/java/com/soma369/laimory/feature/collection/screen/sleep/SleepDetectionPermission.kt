package com.soma369.laimory.feature.collection.screen.sleep

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * 수면 자동 감지 권한 판정.
 *
 * Sleep API 구독에는 활동 인식(`ACTIVITY_RECOGNITION`, Q+ 런타임 권한)이 필요하다. HC 쓰기(WRITE_SLEEP)는
 * 자체 권한 모델이라 [HealthPermissions.sleepProducer] 로 별도 요청한다. 감지 온보딩은 두 권한을 순차로 받는다.
 */
internal object SleepDetectionPermission {
    /** 활동 인식 런타임 권한 문자열. */
    fun recognition(): String = Manifest.permission.ACTIVITY_RECOGNITION

    /** 활동 인식 권한 보유 여부(Q 미만은 설치 시 부여되므로 항상 true). */
    fun hasRecognition(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    /** 런타임 요청이 필요한지(Q+ 이면서 아직 미허용). */
    fun needsRequest(context: Context): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasRecognition(context)
}
