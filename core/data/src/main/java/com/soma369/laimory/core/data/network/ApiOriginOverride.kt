package com.soma369.laimory.core.data.network

/**
 * 빌드 값(`BuildConfig.BASE_URL`) 대신 붙을 서버 origin.
 *
 * **debug 소스셋만 구현을 바인딩한다.** release·qa 에는 구현이 없어 바인딩이 비고, 앱은 빌드 값을 그대로
 * 쓴다 — 서버를 고르는 코드와 다른 서버 주소가 운영 산출물에 실리지 않게 하려는 것이다.
 *
 * 값은 프로세스가 사는 동안 바뀌지 않아야 한다. Retrofit·OAuth 시작 주소·로그인 콜백 host 가 모두 이 값을
 * 한 번 읽고 고정하므로, 바꾸려면 저장한 뒤 프로세스를 다시 시작한다.
 */
interface ApiOriginOverride {
    /** 도메인 루트. `https://host/` 처럼 scheme 을 포함하고 `/` 로 끝난다. */
    val baseUrl: String
}
