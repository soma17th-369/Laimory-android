package com.soma369.laimory.core.domain.model.analytics

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * 기록 날짜가 오늘과 어떤 관계인지.
 *
 * 실제 날짜는 보내지 않는다. 리텐션은 "오늘·어제 기록"만 인정하고 과거 기록 채워 넣기는 빼야 해서
 * 관계만 있으면 충분하다.
 */
enum class AnalyticsRecordDayRelation {
    TODAY,
    YESTERDAY,
    OLDER,
    FUTURE,
    ;

    companion object {
        /**
         * 날짜 경계는 기기 시간대가 아니라 **서울**로 본다. 기기 시간대를 따르면 해외에서 쓴 기록의
         * 오늘·어제가 대시보드의 날짜(서울 기준)와 어긋난다.
         */
        val DAY_BOUNDARY_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

        fun of(
            recordDate: LocalDate,
            clock: Clock,
        ): AnalyticsRecordDayRelation {
            val today = LocalDate.now(clock.withZone(DAY_BOUNDARY_ZONE))
            return when {
                recordDate == today -> TODAY
                recordDate == today.minusDays(1) -> YESTERDAY
                recordDate.isBefore(today) -> OLDER
                else -> FUTURE
            }
        }
    }
}
