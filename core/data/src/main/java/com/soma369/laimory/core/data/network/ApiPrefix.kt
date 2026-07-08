package com.soma369.laimory.core.data.network

/**
 * 서버 API prefix 정책. 앱은 Public 범위만 사용한다.
 *
 * - Public `/api/{applicationVersion}` — 인증 불필요
 * - Auth `/a/api/{applicationVersion}` — 사용자 인증 필요. 인증 도입 시 이 object 에 조합을 추가한다.
 * - `/s/api/{applicationVersion}` 서버 간 API 는 앱 구현 범위 밖.
 *
 * `{applicationVersion}` 은 빌드 설정(API_APP_VERSION)에서 주입한다 — API 코드에 버전값을 분산 고정하지 않는다.
 */
object ApiPrefix {
    /** Public API baseUrl — `{도메인 루트}/api/{applicationVersion}/`. Retrofit 규칙대로 `/` 로 끝난다. */
    fun publicBaseUrl(
        rootUrl: String,
        applicationVersion: String,
    ): String = "${rootUrl.trimEnd('/')}/api/$applicationVersion/"
}
