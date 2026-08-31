package com.soma369.laimory.core.util.permission

import android.content.Context
import android.content.pm.PackageManager

/**
 * 런타임 권한 허용 여부.
 *
 * 이 모듈의 판정은 모두 이 한 줄을 거친다 — 화면마다 `checkSelfPermission` 을 다시 부르면
 * 같은 권한을 두고 판정이 갈라진다.
 */
fun Context.isGranted(permission: String): Boolean = checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
