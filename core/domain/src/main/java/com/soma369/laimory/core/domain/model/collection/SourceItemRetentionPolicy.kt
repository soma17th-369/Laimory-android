package com.soma369.laimory.core.domain.model.collection

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** 앱 빌드 타입 경계에서 주입되는 SourceItem 보존 일수. 오늘을 포함한다. */
data class SourceItemRetentionConfig(
    val retentionDays: Int,
) {
    init {
        require(retentionDays > 0) { "retentionDays must be greater than zero" }
    }
}

/**
 * 실행 시점의 기기 날짜와 시간대를 기준으로 SourceItem 삭제 경계를 계산한다.
 *
 * 시간 단위(24시간 × N)가 아닌 캘린더 날짜 단위 정책이다. 예를 들어 30일 보존이면 오늘과
 * 직전 29개 날짜를 유지하고, 그보다 앞선 날짜의 시작 시각을 [cutoff]로 반환한다.
 */
@Singleton
class SourceItemRetentionPolicy
    @Inject
    constructor(
        private val config: SourceItemRetentionConfig,
        private val clock: Clock,
        private val zoneIdProvider: @JvmSuppressWildcards (() -> ZoneId),
    ) {
        fun cutoff(): Instant {
            val zoneId = zoneIdProvider()
            return java.time.LocalDate
                .now(clock.withZone(zoneId))
                .minusDays(config.retentionDays - 1L)
                .atStartOfDay(zoneId)
                .toInstant()
        }
    }
