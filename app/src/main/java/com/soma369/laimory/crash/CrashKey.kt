package com.soma369.laimory.crash

/**
 * 크래시 리포트를 가르는 꼬리표 이름.
 *
 * 최소로 둔다. 꼬리표는 리포트를 좁히는 데 쓰는 것이지 상태를 중계하는 자리가 아니고, 늘어날수록
 * 개인정보가 새어 나갈 표면이 넓어진다.
 *
 * buildType 은 두지 않는다 — `.debug` · `.qa` · 운영은 applicationId 가 달라 Firebase 앱이 각각
 * 잡히고 리포트도 그만큼 갈린다. 어느 빌드에서 터졌는지는 리포트가 어디에 쌓였는지로 이미 안다.
 */
internal object CrashKey {
    /** 로그인 상태. 로그인 전에만 나는 크래시인지 가른다. */
    const val SIGNED_IN = "signed_in"

    /** 마지막으로 올라온 화면 경로. 어느 화면에서 터졌는지 좁힌다. */
    const val ROUTE = "route"
}
