package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.TimelineEventUpdateException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 통합 PATCH로 Event를 수정하고 성공 응답을 현재 타임라인 세션에 원자 반영한다. */
@Singleton
class UpdateTimelineEventUseCase
    @Inject
    constructor(
        private val repository: TimelineRecordRepository,
        private val sessionRepository: TimelineRecordSessionRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(command: UpdateTimelineEventCommand): Result<TimelineEvent> =
            try {
                execute {
                    try {
                        repository.updateEvent(command).also {
                            // 제자리 치환은 인덱스를 유지해서, 시각을 고쳐도 목록 위치가 그대로다.
                            // 서버가 정한 순서를 그대로 받도록 기록을 다시 읽는다.
                            refreshTimelineRecordSession(
                                sessionRepository.timeline.value?.recordDate,
                                repository,
                                sessionRepository,
                            )
                        }
                    } catch (exception: ApiException) {
                        val reason = TimelineEventUpdateException.reasonOf(exception.errorCode) ?: throw exception
                        throw TimelineEventUpdateException(reason, exception)
                    }
                }
            } catch (exception: TimelineEventUpdateException) {
                Result.failure(exception)
            }
    }
