package com.soma369.laimory.core.domain.model.update

import java.time.Duration
import java.time.Instant

/**
 * 사용자가 미뤄 둔 권장 업데이트.
 *
 * 버전과 시각을 **함께** 기억한다. 시각만 기억하면 보류 기간 안에 올라온 새 권장 버전까지 함께
 * 숨고, 버전만 기억하면 영영 다시 뜨지 않는다.
 */
data class DismissedRecommendation(
    val version: Int,
    val at: Instant,
)

/** 재노출 주기. */
val RECOMMENDATION_SNOOZE: Duration = Duration.ofHours(24)

/**
 * 이 보류가 [version] 권장을 아직 숨기고 있는지.
 *
 * 보류한 버전보다 높은 권장이 오면 기간과 무관하게 보여 준다 — 미룬 것은 그 버전이지 앞으로의
 * 모든 업데이트가 아니다.
 *
 * 기기 시계가 뒤로 돌아가 보류 시각이 미래가 되면 숨기지 않는다. 얼마나 지났는지 알 수 없는
 * 기록으로 안내를 계속 감추는 것보다 한 번 더 보여 주는 편이 낫다.
 */
fun DismissedRecommendation?.hides(
    version: Int,
    now: Instant,
): Boolean {
    if (this == null || this.version < version) return false
    val elapsed = Duration.between(at, now)
    return !elapsed.isNegative && elapsed < RECOMMENDATION_SNOOZE
}
