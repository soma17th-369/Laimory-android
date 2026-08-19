package com.soma369.laimory.core.domain.model.timeline

/**
 * 초안 상태 조회 주기와 장기 처리 전환 경계.
 *
 * 완료는 FCM이 주 경로이고 폴링은 FCM 유실·지연 시의 복구 경로다. 그래서 조회 간격을 촘촘히 두지
 * 않으며, FCM이 유실되면 완료 확인이 최대 한 주기만큼 늦어질 수 있는 것을 제품 결정으로 수용한다.
 */
data class DraftPollingPolicy(
    val intervalMillis: Long = DEFAULT_INTERVAL_MILLIS,
    val longRunningSeconds: Long = DEFAULT_LONG_RUNNING_SECONDS,
) {
    init {
        require(intervalMillis > 0) { "polling interval은 0보다 커야 합니다." }
        require(longRunningSeconds > 0) { "장기 처리 경계는 0보다 커야 합니다." }
    }

    companion object {
        const val DEFAULT_INTERVAL_MILLIS = 60_000L
        const val DEFAULT_LONG_RUNNING_SECONDS = 10L * 60L
    }
}
