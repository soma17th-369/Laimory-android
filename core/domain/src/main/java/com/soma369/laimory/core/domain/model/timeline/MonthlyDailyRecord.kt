package com.soma369.laimory.core.domain.model.timeline

import java.time.LocalDate

/**
 * 캘린더 탐색용 하루 기록 요약.
 *
 * 월별 조회는 날짜·상태·감정만 내려주므로 Event·Item graph 를 가진 [DailyTimeline] 과 다른 타입이다.
 * 기록 상세는 날짜 단건 조회로 다시 받으며 이 모델은 정본이 아니다.
 *
 * [status] 는 서버가 주는 값이 정본이다. 앱이 [emotion] 으로 초안 여부를 **추정하지 않는다** —
 * 감정은 초안/저장과 다른 축이라 감정 없는 저장 기록도 있다. 서버가 필드를 빼거나 모르는
 * literal 을 보내면 null 이며, 그때는 초안이 아닌 것으로 다룬다.
 *
 * [emotion] 이 null 이면 감정이 아직 없는 기록이고, [TimelineEmotion.UNKNOWN] 이면 서버가 모르는
 * literal 을 보낸 경우다.
 */
data class MonthlyDailyRecord(
    val recordDate: LocalDate,
    val status: DailyRecordStatus?,
    val emotion: TimelineEmotion?,
)
