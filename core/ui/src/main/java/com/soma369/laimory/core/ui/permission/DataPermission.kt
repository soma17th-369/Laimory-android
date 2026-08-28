package com.soma369.laimory.core.ui.permission

/**
 * 하루 기록에 쓰는 데이터의 플랫폼 권한.
 *
 * 온보딩과 설정의 `데이터 권한` 이 같은 목록을 본다 — 두 화면이 따로 판정하면 한쪽은 `연결됨`,
 * 다른 쪽은 아니라고 나온다.
 *
 * 여기서는 **무엇을 요청할지만** 가리킨다. 실제 판정과 요청은 Android 경계
 * (`core:util` 의 permission)가 맡는다 — 페이지 목록이 `Context` 나 permission 문자열을
 * 들고 있으면 목록을 고칠 때마다 Android 를 알아야 한다.
 */
enum class DataPermission {
    /** 사진 읽기. 전체·일부 허용을 모두 허용으로 본다. */
    PHOTO,

    /** 일정 읽기. */
    CALENDAR,

    /** 전경 → 백그라운드 → 활동 인식 순서로 진행하는 위치. */
    LOCATION,

    /** 알림 읽기(NotificationListener). 요청 다이얼로그가 없어 시스템 설정으로 보낸다. */
    NOTIFICATION_LISTENER,

    /** Laimory 가 알림을 표시할 권한. */
    APP_NOTIFICATION,

    /**
     * Health Connect 걸음수·수면 읽기.
     *
     * Android 권한이 아니라 Health Connect 앱이 가진 권한이다 — 그 앱이 없으면 요청 자체가
     * 성립하지 않아 `거부` 와 `쓸 수 없는 기기` 를 구분해야 한다.
     */
    HEALTH,
}
