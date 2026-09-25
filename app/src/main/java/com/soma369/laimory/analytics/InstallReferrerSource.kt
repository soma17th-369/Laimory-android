package com.soma369.laimory.analytics

/** 설치 referrer 를 한 번 조회한다. 재시도는 부르는 쪽이 정한다. */
internal fun interface InstallReferrerSource {
    suspend fun fetch(): InstallReferrerLookup
}

internal sealed interface InstallReferrerLookup {
    /**
     * 조회에 성공했다. 시각은 Play 가 준 초 단위 값이고, 없으면 0 이다.
     *
     * data class 로 두지 않는다 — 자동 `toString` 이 원문 referrer 를 찍으면 로그 한 줄로 샌다.
     */
    class Found(
        val referrer: String?,
        val clickAtSeconds: Long,
        val installBeginAtSeconds: Long,
    ) : InstallReferrerLookup {
        override fun toString(): String = "Found"
    }

    /** 이 기기·스토어에서는 제공하지 않는다. 다시 물어도 같다. */
    data object Unsupported : InstallReferrerLookup

    /** 서비스가 잠시 응답하지 않거나 연결이 끊겼다. 다시 물으면 될 수 있다. */
    data object Transient : InstallReferrerLookup
}
