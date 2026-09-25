package com.soma369.laimory.core.domain.usecase.analytics

import com.soma369.laimory.core.domain.model.analytics.AnalyticsEventOrigin
import com.soma369.laimory.core.domain.repository.AnalyticsTimelineEditLogRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 완료 전 이벤트 편집을 완료 요약용 흔적으로 남긴다.
 *
 * 작성 중인 기록에서만 부른다. 완료한 기록을 고친 것은 이미 보낸 요약에 들어갈 수 없고, 남겨 두면
 * 비울 계기가 없다.
 */
@Singleton
class RecordTimelineEditUseCase
    @Inject
    constructor(
        private val repository: AnalyticsTimelineEditLogRepository,
    ) {
        /** 메모가 아닌 내용을 고쳤다. 메모는 완료 순간의 값으로 따로 센다. */
        suspend fun edited(
            recordDate: LocalDate,
            timelineEventId: Long,
        ) {
            repository.markEdited(recordDate, timelineEventId)
        }

        /** 이벤트를 지웠다. 지운 뒤에는 출처를 알 수 없으므로 지우기 전 질문으로 가른다. */
        suspend fun deleted(
            recordDate: LocalDate,
            timelineEventId: Long,
            question: String?,
        ) {
            if (AnalyticsEventOrigin.of(question) != AnalyticsEventOrigin.AI) return
            repository.markDeletedAi(recordDate, timelineEventId)
        }
    }
