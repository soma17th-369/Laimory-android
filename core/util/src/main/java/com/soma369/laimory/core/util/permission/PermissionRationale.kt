package com.soma369.laimory.core.util.permission

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * 시스템이 이 권한을 **더 이상 묻지 않는지** 판정한다.
 *
 * Android 는 사용자가 두 번 거부하면 요청을 조용히 삼킨다 — 런처는 즉시 결과를 돌려주고
 * 다이얼로그는 뜨지 않는다. 이걸 모르면 화면은 `허용하기` 버튼을 계속 보여 주고, 누르면
 * 아무 일도 일어나지 않는 막다른 길이 된다.
 *
 * **한 번도 요청하지 않은 상태와 구분되지 않는다**(둘 다 rationale 이 false 다). 그래서
 * 요청을 보낸 **직후에만** 이 판정을 쓴다 — 그 시점의 false 는 "물어봤는데 안 떴다" 는 뜻이다.
 */
fun Context.isPermanentlyDenied(permissions: Array<String>): Boolean {
    val activity = findActivity() ?: return false
    return permissions.none { isGranted(it) } &&
        permissions.none { activity.shouldShowRequestPermissionRationale(it) }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
