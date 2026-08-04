package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.TimelineEventUpdateException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 전용 PUT으로 Event 메모를 수정하고 서버 최신 Event를 현재 타임라인 세션에 반영한다. */
@Singleton
class UpdateTimelineEventMemoUseCase
    @Inject
    constructor(
        private val repository: TimelineRecordRepository,
        private val sessionRepository: TimelineRecordSessionRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(
            timelineEventId: Long,
            memo: String?,
        ): Result<TimelineEvent> =
            try {
                execute {
                    try {
                        repository
                            .updateEventMemo(timelineEventId, memo)
                            .also(sessionRepository::replaceEvent)
                    } catch (exception: ApiException) {
                        val reason = exception.toUpdateReason() ?: throw exception
                        throw TimelineEventUpdateException(reason, exception)
                    }
                }
            } catch (exception: TimelineEventUpdateException) {
                Result.failure(exception)
            }

        private fun ApiException.toUpdateReason(): TimelineEventUpdateException.Reason? =
            when (errorCode) {
                INVALID_REQUEST_ERROR_CODE -> TimelineEventUpdateException.Reason.INVALID_REQUEST
                EVENT_UNAVAILABLE_ERROR_CODE -> TimelineEventUpdateException.Reason.EVENT_UNAVAILABLE
                RECORD_SAVED_ERROR_CODE -> TimelineEventUpdateException.Reason.RECORD_ALREADY_SAVED
                else -> null
            }

        private companion object {
            const val INVALID_REQUEST_ERROR_CODE = -400
            const val EVENT_UNAVAILABLE_ERROR_CODE = -404
            const val RECORD_SAVED_ERROR_CODE = -1003
        }
    }
