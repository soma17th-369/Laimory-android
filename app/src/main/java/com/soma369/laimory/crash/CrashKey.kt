package com.soma369.laimory.crash

/**
 * 크래시 리포트를 가르는 꼬리표 이름.
 *
 * 두 종류만 둔다. **리포트를 좁히는 것**(어느 화면, 로그인 전후)과 **크래시보다 한참 앞서 배경에서
 * 시작된 상태**(위치 수집, 수면 감지, 알림 접근, 초안 작업). 뒤의 것은 로그로 남기면 몇 시간 사이
 * 브레드크럼 창(최근 64KB) 밖으로 밀려나 크래시 시점에는 보이지 않는다. 키는 창과 무관하게 남는다.
 *
 * 그 밖으로는 늘리지 않는다. 늘어날수록 개인정보가 새어 나갈 표면이 넓어지고 상한(64개)도 있다.
 * 값은 **고정된 몇 개의 토큰**만 쓴다 — 식별자·날짜·시각·좌표를 넣지 않는다.
 *
 * buildType 은 두지 않는다 — `.debug` · `.qa` · 운영은 applicationId 가 달라 Firebase 앱이 각각
 * 잡히고 리포트도 그만큼 갈린다. 어느 빌드에서 터졌는지는 리포트가 어디에 쌓였는지로 이미 안다.
 */
internal object CrashKey {
    /** 로그인 상태. 로그인 전에만 나는 크래시인지 가른다. */
    const val SIGNED_IN = "signed_in"

    /** 마지막으로 올라온 화면 경로. 어느 화면에서 터졌는지 좁힌다. */
    const val ROUTE = "route"

    /** 위치 수집. `off`(사용자가 끔) · `on` · `no_permission`(켜 뒀지만 권한이 없어 돌 수 없음). */
    const val LOCATION_TRACKING = "location_tracking"

    /** 수면 자동 감지를 켜 뒀는지. `on` · `off`. */
    const val SLEEP_DETECTION = "sleep_detection"

    /** 알림 읽기 접근. `granted` · `denied`. */
    const val NOTIFICATION_ACCESS = "notification_access"

    /**
     * 추적 중인 초안 작업의 단계. `idle` · `processing` · `long_running` · `success` · `failed` ·
     * `retryable_error` · `unavailable`.
     *
     * 활성 작업은 전경에 들어올 때 복원되므로, 전경을 거치지 않은 프로세스에서는 저장된 작업이 있어도 `idle` 이다.
     */
    const val DRAFT_TASK = "draft_task"
}
