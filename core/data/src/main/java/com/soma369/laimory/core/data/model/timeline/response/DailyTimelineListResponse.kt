package com.soma369.laimory.core.data.model.timeline.response

import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import kotlinx.serialization.Serializable

/**
 * `GET /timeline/daily-records` 응답.
 *
 * 서버 정렬(`recordDate` 내림차순, 동일 날짜는 `dailyRecordId` 내림차순)을 그대로 보존하며,
 * 결과가 없으면 [timelines]는 빈 목록이다.
 */
@Serializable
data class DailyTimelineListResponse(
    val timelines: List<DailyTimelineResponse>,
)

internal fun DailyTimelineListResponse.toDomain(): List<DailyTimeline> = timelines.map(DailyTimelineResponse::toDomain)
