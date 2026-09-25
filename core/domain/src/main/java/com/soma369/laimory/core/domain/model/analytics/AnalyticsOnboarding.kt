package com.soma369.laimory.core.domain.model.analytics

/**
 * 승인된 온보딩 구성의 판. 장의 순서나 구성이 바뀌면 새 값을 더해 옛 데이터와 섞이지 않게 한다.
 */
enum class AnalyticsOnboardingVersion {
    V1,
}

/** 온보딩의 장. 화면의 장 키와 따로 두어 전송값을 고정한다. */
enum class AnalyticsOnboardingStep {
    INTRO,
    PHOTO,
    CALENDAR,
    LOCATION,
    NOTIFICATION,
    APP_NOTIFICATION,
    DONE,
}

/** 장이 어떻게 보이게 됐는지. */
enum class AnalyticsOnboardingEntryMode {
    /** 온보딩을 처음 열어 첫 장이 보였다. */
    INITIAL,

    /** 앱을 다시 켜 마지막으로 본 장에서 이어 열었다. */
    RESUME,

    /** 앞뒤로 넘겨 도착했다. */
    NAVIGATION,
}

/** 장이 보인 순간의 권한 상태. 안내 장(`intro` · `done`)은 [NOT_APPLICABLE] 이다. */
enum class AnalyticsOnboardingEligibility {
    /** 앱 안에서 요청할 수 있다. 위치를 `사용 중에만` 허용해 `항상 허용` 이 남은 경우도 여기다. */
    NEEDS_REQUEST,

    /** 이미 쓸 수 있다. 요청이 없는 기기(Android 12 이하의 앱 알림)도 여기다. */
    ALREADY_USABLE,

    /** 시스템 설정에서만 켤 수 있다 — 알림 접근, 두 번 거부해 시스템이 더 묻지 않는 권한. */
    SETTINGS_ONLY,

    /** 이 기기에는 켤 방법이 없다. */
    NOT_SUPPORTED,

    NOT_APPLICABLE,
}

/** 장에서 고른 행동. */
enum class AnalyticsOnboardingAction {
    /** `나중에`. */
    SKIP,

    /** 주 CTA 로 넘겼거나, 허용이 끝나 자동으로 넘어갔다. */
    NEXT,

    /** 상단 뒤로·시스템 뒤로. */
    BACK,

    /** 마지막 장에서 약관 동의 기록에 성공했다. */
    FINISH,
}
