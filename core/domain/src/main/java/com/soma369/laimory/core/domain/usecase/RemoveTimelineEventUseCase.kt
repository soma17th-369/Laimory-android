package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import javax.inject.Inject

class RemoveTimelineEventUseCase
    @Inject
    constructor(
        private val repository: TimelineRecordSessionRepository,
    ) {
        operator fun invoke(timelineEventId: Long) = repository.removeEvent(timelineEventId)
    }
