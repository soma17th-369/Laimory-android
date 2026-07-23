package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.TimelineRecordDeleteException
import com.soma369.laimory.core.domain.exception.toTimelineRecordDeleteExceptionOrNull
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 서버에서 Event 삭제가 성공한 뒤 현재 타임라인 세션에서 해당 Event만 제거한다. */
@Singleton
class DeleteTimelineEventUseCase
    @Inject
    constructor(
        private val repository: TimelineRecordRepository,
        private val sessionRepository: TimelineRecordSessionRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(timelineEventId: Long): Result<Unit> =
            try {
                execute {
                    try {
                        repository.deleteEvent(timelineEventId)
                        sessionRepository.removeEvent(timelineEventId)
                    } catch (exception: ApiException) {
                        throw exception.toTimelineRecordDeleteExceptionOrNull() ?: exception
                    }
                }
            } catch (exception: TimelineRecordDeleteException) {
                Result.failure(exception)
            }
    }
