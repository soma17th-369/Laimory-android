package com.soma369.laimory.core.domain.usecase.analytics

import com.soma369.laimory.core.domain.model.analytics.AnalyticsTimelineEditLog
import com.soma369.laimory.core.domain.repository.AnalyticsTimelineEditLogRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** 완료 요약을 만들 때 편집 흔적을 꺼내고 비운다. 기록을 지울 때도 불러 흔적을 치운다. */
@Singleton
class TakeTimelineEditLogUseCase
    @Inject
    constructor(
        private val repository: AnalyticsTimelineEditLogRepository,
    ) {
        suspend operator fun invoke(recordDate: LocalDate): AnalyticsTimelineEditLog = repository.take(recordDate)
    }
