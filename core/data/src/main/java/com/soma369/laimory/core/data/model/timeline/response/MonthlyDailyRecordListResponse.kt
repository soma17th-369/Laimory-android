package com.soma369.laimory.core.data.model.timeline.response

import com.soma369.laimory.core.domain.model.timeline.MonthlyDailyRecord
import kotlinx.serialization.Serializable

/**
 * `GET /timeline/monthly-records?year=&month=` 응답.
 *
 * 해당 월의 DRAFT·SAVED 기록을 `recordDate` 오름차순으로 모두 포함하며, 기록이 없는 월은 빈 목록이다.
 */
@Serializable
data class MonthlyDailyRecordListResponse(
    val dailyRecords: List<MonthlyDailyRecordResponse>,
)

internal fun MonthlyDailyRecordListResponse.toDomain(): List<MonthlyDailyRecord> = dailyRecords.map(MonthlyDailyRecordResponse::toDomain)
