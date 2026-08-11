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

    /** 건수 표기. 원본과 전송 건수가 다르면 관계를 드러내고, 0건 유형은 숨기지 않고 명시한다. */
    val countLabel: String
        get() =
            when {
                sentCount == 0 -> "0건 · 전송되지 않음"
                originalCount != sentCount -> "수집 ${originalCount}건 중 ${sentCount}건 전송"
                else -> "${sentCount}건 전송"
            }
}
