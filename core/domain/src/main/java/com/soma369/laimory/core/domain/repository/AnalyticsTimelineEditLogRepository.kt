package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.analytics.AnalyticsTimelineEditLog
import java.time.LocalDate

/**
 * 완료 전 편집 흔적을 기록 날짜별로 남긴다.
 *
 * 분석용이라 실패가 앱 동작을 막지 않는다 — 구현은 저장·읽기 실패를 삼키고, 읽지 못하면 빈 흔적을 준다.
 */
interface AnalyticsTimelineEditLogRepository {
    suspend fun markEdited(
        recordDate: LocalDate,
        timelineEventId: Long,
    )

    suspend fun markDeletedAi(
        recordDate: LocalDate,
        timelineEventId: Long,
    )

    /** 흔적을 읽고 비운다. 완료 요약은 한 번만 만들고, 기록을 지우면 흔적도 쓸 데가 없다. */
    suspend fun take(recordDate: LocalDate): AnalyticsTimelineEditLog

    /** 남은 흔적을 날짜와 상관없이 모두 비운다. 로그아웃처럼 그 사람의 작성이 끝났을 때 쓴다. */
    suspend fun clear()
}
