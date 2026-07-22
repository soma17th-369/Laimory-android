package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import javax.inject.Inject

class ReplaceTimelineEventUseCase
    @Inject
    constructor(
        private val repository: TimelineRecordSessionRepository,
    ) {
        operator fun invoke(event: TimelineEvent) = repository.replaceEvent(event)
    }
