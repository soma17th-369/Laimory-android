package com.soma369.laimory.core.domain.model.analytics

import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 지난 기록이 오늘에서 며칠 전인지의 구간.
 *
 * 날짜 경계는 [AnalyticsRecordDayRelation] 과 같은 서울이다.
 */
enum class AnalyticsRecordAgeBucket {
    /** 어제. */
    D1,

    /** 2~6일 전. */
    D2_6,

    /** 7~29일 전. */
    D7_29,

    /** 30일 이상 전. */
    D30_PLUS,
    ;

    companion object {
        /** 오늘이거나 미래면 지난 기록이 아니라 null. */
        fun of(
            recordDate: LocalDate,
            clock: Clock,
        ): AnalyticsRecordAgeBucket? {
            val today = LocalDate.now(clock.withZone(AnalyticsRecordDayRelation.DAY_BOUNDARY_ZONE))
            val days = ChronoUnit.DAYS.between(recordDate, today)
            return when {
                days < 1 -> null
                days == 1L -> D1
                days < 7 -> D2_6
                days < 30 -> D7_29
                else -> D30_PLUS
            }
        }
    }
}
