package com.soma369.laimory.core.domain.model.timeline

import java.time.LocalDate

/**
 * 캘린더 탐색용 하루 기록 요약.
 *
 * 월별 조회는 날짜와 감정만 내려주므로 Event·Item graph 를 가진 [DailyTimeline] 과 다른 타입이다.
 * 기록 상세는 날짜 단건 조회로 다시 받으며 이 모델은 정본이 아니다.
 *
 * [emotion] 이 null 이면 감정이 아직 없는 기록(DRAFT·legacy SAVED)이고,
 * [TimelineEmotion.UNKNOWN] 이면 서버가 모르는 literal 을 보낸 경우다.
 */
data class MonthlyDailyRecord(
    val recordDate: LocalDate,
    val emotion: TimelineEmotion?,
)
