package com.soma369.laimory.core.data.model.timeline.response

import com.soma369.laimory.core.domain.model.timeline.MonthlyDailyRecord
import kotlinx.serialization.Serializable

/**
 * 월별 조회 항목. 서버는 `recordDate` 와 nullable `emotionType` 두 필드만 내려준다.
 *
 * `dailyRecordId`·`status`·`events` 는 응답에 없다 — 캘린더 셀이 기록 유무와 감정만 그리기 때문이다.
 */
@Serializable
data class MonthlyDailyRecordResponse(
    val recordDate: String,
    val emotionType: String? = null,
)

internal fun MonthlyDailyRecordResponse.toDomain(): MonthlyDailyRecord =
    MonthlyDailyRecord(
        recordDate = recordDate.parseLocalDate("recordDate"),
        emotion = emotionType.toTimelineEmotionOrNull(),
    )
