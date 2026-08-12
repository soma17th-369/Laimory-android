package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable

/**
 * 데이터 유형 1행의 전송 요약.
 *
 * [originalCount]는 기록 창 안의 수집 원본 건수, [sentCount]는 타입별 상한 적용 후
 * 실제 API 요청에 담기는 건수다.
 */
@Immutable
data class DraftConsentTypeSummary(
    val group: DraftConsentTypeGroup,
    val originalCount: Int,
    val sentCount: Int,
    val sections: List<DraftConsentDetailSection>,
) {
    val isSent: Boolean get() = sentCount > 0

    /**
     * 건수 표기. 원본과 전송 건수가 다르면 관계를 드러내고, 0건 유형은 숨기지 않고 명시한다.
     * [includedCount]는 사용자 제외를 반영한 실제 전송 예정 건수다(기본값은 제외 없음).
     */
    fun countLabel(includedCount: Int = sentCount): String {
        val unit = group.countUnit
        return when {
            includedCount == 0 -> "0$unit · 전송되지 않음"
            originalCount != includedCount -> "수집 $originalCount$unit 중 $includedCount$unit 전송"
            else -> "$includedCount$unit 포함"
        }
    }
}
