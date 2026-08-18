package com.soma369.laimory.feature.home.state

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 초안 기록 범위가 지켜야 하는 제약.
 *
 * 홈·초안 경계에만 둔다 — `RecordDateWindow` 는 사진 조회·수집 아이템 선별 등이 함께 쓰는 공용
 * 모델이라 여기에 최소 길이를 넣으면 그 사용처들이 함께 좁아진다.
 *
 * - 시작은 기록 날짜 당일 `00:00 ~ 23:59`(시작일 자체를 옮기지 않는다)
 * - 종료는 당일 `06:00` 부터 익일 `06:00` 까지
 * - 두 시각의 간격은 [MIN_DURATION] 이상
 */
object DraftWindowPolicy {
    /** 기록 범위의 최소 길이. */
    val MIN_DURATION: Duration = Duration.ofHours(6)

    /** 종료로 고를 수 있는 가장 이른 시각(기록 날짜 당일). */
    val END_EARLIEST_TIME: LocalTime = LocalTime.of(6, 0)

    /** 종료로 고를 수 있는 가장 늦은 시각(익일). */
    val END_LATEST_TIME: LocalTime = LocalTime.of(6, 0)

    /**
     * 시작 시각을 정한 뒤 종료로 고를 수 있는 범위.
     *
     * 최소 길이 때문에 시작이 늦어질수록 종료의 하한도 함께 밀린다 — 예를 들어 시작이 `23:55` 면
     * 종료는 익일 `05:55 ~ 06:00` 만 남는다.
     */
    fun endRange(
        recordDate: LocalDate,
        startTime: LocalTime,
    ): ClosedRange<LocalDateTime> {
        val earliest = recordDate.atTime(END_EARLIEST_TIME)
        val minimum = recordDate.atTime(startTime) + MIN_DURATION
        val latest = recordDate.plusDays(1).atTime(END_LATEST_TIME)
        return maxOf(earliest, minimum)..latest
    }

    /** 시작·종료 조합이 범위와 최소 길이를 모두 지키는지. */
    fun isValid(
        recordDate: LocalDate,
        startTime: LocalTime,
        endDateTime: LocalDateTime,
    ): Boolean = endDateTime in endRange(recordDate, startTime)

    /** 범위를 벗어난 종료를 가까운 경계로 붙인다. 시작을 옮겨 종료가 밀려날 때 쓴다. */
    fun coerceEnd(
        recordDate: LocalDate,
        startTime: LocalTime,
        endDateTime: LocalDateTime,
    ): LocalDateTime = endDateTime.coerceIn(endRange(recordDate, startTime))
}
