package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.TimelineEventPhotoDeleteException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DeleteTimelineEventPhotoOutcome {
    /** DELETE가 성공해 세션에서도 대상 Item을 제거했다. */
    data object Deleted : DeleteTimelineEventPhotoOutcome

    /** DELETE 404 뒤 현재 DailyRecord를 재조회해 최신 서버 상태로 교체했다. */
    data object Reconciled : DeleteTimelineEventPhotoOutcome

    /** 재조회한 DailyRecord에 대상 Event가 더 이상 존재하지 않는다. */
    data object EventUnavailable : DeleteTimelineEventPhotoOutcome
}

/** Event와 PHOTO Item의 연결을 해제하고 현재 타임라인 세션을 서버 결과에 맞춰 갱신한다. */
@Singleton
class DeleteTimelineEventPhotoUseCase
    @Inject
    constructor(
        private val repository: TimelineRecordRepository,
        private val sessionRepository: TimelineRecordSessionRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(
            timelineEventId: Long,
            timelineItemId: Long,
        ): Result<DeleteTimelineEventPhotoOutcome> =
            try {
                execute {
                    try {
                        repository.deleteEventPhoto(timelineEventId, timelineItemId)
                        sessionRepository.removeEventItem(timelineEventId, timelineItemId)
                        DeleteTimelineEventPhotoOutcome.Deleted
                    } catch (exception: ApiException) {
                        when (exception.errorCode) {
                            TARGET_UNAVAILABLE_ERROR_CODE -> reconcileTimeline(timelineEventId)
                            ITEM_NOT_PHOTO_ERROR_CODE ->
                                throw TimelineEventPhotoDeleteException(
                                    TimelineEventPhotoDeleteException.Reason.ITEM_NOT_PHOTO,
                                    exception,
                                )
                            else -> throw exception
                        }
                    }
                }
            } catch (exception: TimelineEventPhotoDeleteException) {
                Result.failure(exception)
            }

        private suspend fun reconcileTimeline(timelineEventId: Long): DeleteTimelineEventPhotoOutcome {
            val recordDate =
                sessionRepository.timeline.value?.recordDate
                    ?: return DeleteTimelineEventPhotoOutcome.EventUnavailable
            val timeline =
                try {
                    repository.getDailyRecord(recordDate)
                } catch (exception: ApiException) {
                    if (exception.errorCode == TARGET_UNAVAILABLE_ERROR_CODE) {
                        sessionRepository.clear()
                        return DeleteTimelineEventPhotoOutcome.EventUnavailable
                    }
                    throw exception
                }
            sessionRepository.save(timeline)
            return if (timeline.events.any { it.timelineEventId == timelineEventId }) {
                DeleteTimelineEventPhotoOutcome.Reconciled
            } else {
                DeleteTimelineEventPhotoOutcome.EventUnavailable
            }
        }

        private companion object {
            const val TARGET_UNAVAILABLE_ERROR_CODE = -404
            const val ITEM_NOT_PHOTO_ERROR_CODE = -1018
        }
    }
