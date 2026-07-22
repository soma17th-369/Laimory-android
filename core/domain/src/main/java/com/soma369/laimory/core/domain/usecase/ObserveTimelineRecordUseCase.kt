package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveTimelineRecordUseCase
    @Inject
    constructor(
        private val repository: TimelineRecordSessionRepository,
    ) {
        operator fun invoke(): StateFlow<DailyTimeline?> = repository.timeline
    }
