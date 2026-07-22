package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import javax.inject.Inject

class ClearTimelineRecordUseCase
    @Inject
    constructor(
        private val repository: TimelineRecordSessionRepository,
    ) {
        operator fun invoke() = repository.clear()
    }
