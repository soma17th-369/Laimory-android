package com.soma369.laimory.core.ui.permission

/**
 * 지금 이 소스에 대해 사용자가 할 수 있는 **하나의** 행동.
 *
 * 시트에 버튼을 하나만 두기 위한 값이다. 소스마다 갈 수 있는 곳이 달라서 — 런타임 다이얼로그,
 * 앱 상세 설정, 알림 접근 설정 — 화면이 그 차이를 알면 소스가 늘 때마다 화면을 고쳐야 한다.
 *
 * 실제 실행은 [DataPermissionState.act] 하나로 통일한다. 여기서는 **버튼 문구를 고르는
 * 근거**만 준다.
 */
enum class DataPermissionAction {
    /** 시스템 권한 다이얼로그를 띄운다. */
    REQUEST,

    /** 사진을 다시 고르게 한다(일부 허용 상태에서 범위를 넓히는 경로). */
    RESELECT_PHOTOS,

    /** 앱 상세 설정으로 보낸다. Android 11+ 백그라운드 위치는 여기서만 켤 수 있다. */
    APP_SETTINGS,

    /** 알림 접근 설정으로 보낸다. 이 권한은 다이얼로그가 없다. */
    LISTENER_SETTINGS,

    /** Health Connect 의 권한 화면으로 보낸다. 헬스 권한은 앱 설정에 나오지 않는다. */
    HEALTH_SETTINGS,

    /** 할 수 있는 것이 없다. 기기가 지원하지 않아 열 방법 자체가 없다. */
    NONE,
}
