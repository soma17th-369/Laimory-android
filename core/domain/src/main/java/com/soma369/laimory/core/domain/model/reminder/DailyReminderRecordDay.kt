package com.soma369.laimory.core.domain.model.reminder

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 리마인더가 가리키는 기록 날짜를 정하는 규칙.
 *
 * 하루는 [START] 에 시작한다. 홈의 기본 날짜 규칙과 같다 — 새벽에 연 홈은 어제를 고른다. 알림 시각을 새벽으로
 * 옮긴 사람에게 그 알림은 어제를 마무리하라는 뜻이다.
 *
 * 예약(앱)과 "오늘 이미 받았는지" 안내(설정 화면)가 같은 규칙을 써야 해서 도메인에 둔다. 둘이 따로 세면 안내는
 * "이미 보냈어요"라고 하는데 예약은 오늘 다시 거는 식으로 어긋난다.
 */
object DailyReminderRecordDay {
    val START: LocalTime = LocalTime.of(6, 0)

    /** [dateTime] 이 속한 기록 날짜. */
    fun of(dateTime: LocalDateTime): LocalDate =
        if (dateTime.toLocalTime().isBefore(START)) {
            dateTime.toLocalDate().minusDays(1)
        } else {
            dateTime.toLocalDate()
        }
}
