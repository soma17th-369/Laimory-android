package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 저장된 DailyRecord 전체 목록을 서버 정렬 그대로 조회한다. */
@Singleton
class GetDailyRecordsUseCase
    @Inject
    constructor(
        private val repository: TimelineRecordRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(): Result<List<DailyTimeline>> = execute { repository.getDailyRecords() }
    }
