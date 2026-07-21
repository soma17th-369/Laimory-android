package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.timeline.DraftTaskStatusOutcome
import com.soma369.laimory.core.domain.model.timeline.TimelineErrorCode
import com.soma369.laimory.core.domain.model.timeline.timelineErrorCode
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 초안 생성 작업(비동기)의 진행 상태를 조회한다. */
@Singleton
class GetDraftTaskStatusUseCase
    @Inject
    constructor(
        private val repository: TimelineDraftRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(taskId: String): Result<DraftTaskStatusOutcome> =
            execute {
                try {
                    DraftTaskStatusOutcome.Snapshot(repository.getDraftStatus(taskId))
                } catch (exception: ApiException) {
                    when (exception.timelineErrorCode) {
                        TimelineErrorCode.DRAFT_TASK_NOT_FOUND -> DraftTaskStatusOutcome.TaskUnavailable
                        TimelineErrorCode.RECORD_NOT_FOUND -> DraftTaskStatusOutcome.ResultUnavailable
                        else -> throw exception
                    }
                }
            }
    }
