package com.soma369.laimory.core.domain.model.timeline

/** 초안 상태 조회 주기와 장기 처리 전환 경계. */
data class DraftPollingPolicy(
    val intervalMillis: Long = DEFAULT_INTERVAL_MILLIS,
    val longRunningSeconds: Long = DEFAULT_LONG_RUNNING_SECONDS,
) {
    init {
        require(intervalMillis > 0) { "polling interval은 0보다 커야 합니다." }
        require(longRunningSeconds > 0) { "장기 처리 경계는 0보다 커야 합니다." }
    }

    companion object {
        const val DEFAULT_INTERVAL_MILLIS = 5_000L
        const val DEFAULT_LONG_RUNNING_SECONDS = 10L * 60L
    }
}
