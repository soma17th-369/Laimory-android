package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.timeline.DraftTaskSnapshot
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository

/** 초안 생성 작업(비동기)의 진행 상태를 조회한다. */
class GetDraftTaskStatusUseCase(
    private val repository: TimelineDraftRepository,
    messageHelper: MessageHelper,
) : BaseUseCase(messageHelper) {
    suspend operator fun invoke(taskId: String): Result<DraftTaskSnapshot> = execute { repository.getDraftStatus(taskId) }
}
