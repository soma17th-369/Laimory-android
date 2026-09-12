package com.soma369.laimory.feature.onboarding.model

import com.soma369.laimory.core.ui.permission.DataPermission
import com.soma369.laimory.core.ui.permission.DataPermissionState

/**
 * 권한 장에서 받을 것을 다 받았는지.
 *
 * 위치는 `항상 허용` 까지 받으면 끝이다. 이동수단 인식은 첫 요청에 함께 물었고, 거부해도 속도 추론으로
 * 폴백해 수집을 막지 않는다 — 그것 때문에 장을 붙잡으면 버튼을 한 번 더 눌러야 한다. 다시 켜는 자리는
 * 설정의 위치 시트다.
 *
 * 다른 권한은 허용 여부만 본다.
 */
internal fun DataPermissionState.isPageDone(permission: DataPermission?): Boolean =
    when (permission) {
        null -> false
        DataPermission.LOCATION -> locationStep.collectsInBackground
        else -> isGranted(permission)
    }
