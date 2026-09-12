package com.soma369.laimory.feature.onboarding.model

import com.soma369.laimory.core.ui.permission.DataPermission
import com.soma369.laimory.core.ui.permission.LocationPermissionStep

/**
 * 장과 남은 단계에 맞는 안내.
 *
 * 위치는 창이 두 번 뜬다 — 전경 팝업에서 `앱 사용 중에만 허용`, 이어 열리는 권한 화면에서
 * `항상 허용`. 단계를 보고 안내도 함께 바뀐다.
 *
 * 알림 읽기와 헬스는 `null` 이다. 둘은 창이 아니라 설정 화면으로 가므로 창 모형을 그리면
 * 오히려 다른 화면을 찾게 만든다.
 */
internal fun permissionGuideSpec(
    permission: DataPermission?,
    locationStep: LocationPermissionStep,
): PermissionGuideSpec? =
    when (permission) {
        DataPermission.PHOTO ->
            PermissionGuideSpec(
                caption = "다음 창에서 ‘모두 허용’을 눌러요",
                title = "%s에서 기기의 사진과 동영상에 액세스하도록 허용하시겠습니까?",
                // 첫 줄은 기기의 권한 모듈 버전이 문구를 정한다(Android 14 QPR3 부터 `제한된 액세스 허용`).
                options = listOf("제한된 액세스 허용", "모두 허용", "허용 안함"),
                highlightedIndex = 1,
            )

        DataPermission.CALENDAR ->
            PermissionGuideSpec(
                caption = "다음 창에서 ‘허용’을 눌러요",
                title = "%s에서 캘린더에 액세스하도록 허용하시겠습니까?",
                options = listOf("허용", "허용 안함"),
                highlightedIndex = 0,
            )

        DataPermission.LOCATION ->
            when (locationStep) {
                LocationPermissionStep.FOREGROUND ->
                    PermissionGuideSpec(
                        caption = "다음 창에서 ‘앱 사용 중에만 허용’을 눌러요",
                        title = "%s에서 기기 위치에 액세스하도록 허용하시겠습니까?",
                        options = listOf("앱 사용 중에만 허용", "이번만 허용", "허용 안함"),
                        highlightedIndex = 0,
                    )

                // 팝업이 아니라 이 앱의 위치 권한 화면이 열린다. 그래서 문구가 `창` 이 아니라 `화면` 이다.
                LocationPermissionStep.BACKGROUND ->
                    PermissionGuideSpec(
                        caption = "다음 화면에서 ‘항상 허용’을 눌러요",
                        title = "이 앱의 위치 액세스 권한",
                        options = listOf("항상 허용", "앱 사용 중에만 허용", "매번 확인", "허용 안함"),
                        highlightedIndex = 0,
                    )

                LocationPermissionStep.ACTIVITY ->
                    PermissionGuideSpec(
                        caption = "다음 창에서 ‘허용’을 눌러요",
                        title = "%s에서 신체 활동에 액세스하도록 허용하시겠습니까?",
                        options = listOf("허용", "허용 안함"),
                        highlightedIndex = 0,
                    )

                LocationPermissionStep.GRANTED -> null
            }

        DataPermission.APP_NOTIFICATION ->
            PermissionGuideSpec(
                caption = "다음 창에서 ‘허용’을 눌러요",
                title = "%s에서 알림을 보내도록 허용하시겠습니까?",
                options = listOf("허용", "허용 안함"),
                highlightedIndex = 0,
            )

        DataPermission.NOTIFICATION_LISTENER, DataPermission.HEALTH, null -> null
    }
