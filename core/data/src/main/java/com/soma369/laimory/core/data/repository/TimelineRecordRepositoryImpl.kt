package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.TimelineRecordRemoteDataSource
import com.soma369.laimory.core.data.model.timeline.request.toRequestJson
import com.soma369.laimory.core.data.model.timeline.response.toDomain
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import javax.inject.Inject

class TimelineRecordRepositoryImpl
    @Inject
    constructor(
        private val remote: TimelineRecordRemoteDataSource,
    ) : TimelineRecordRepository {
        override suspend fun updateEvent(command: UpdateTimelineEventCommand): TimelineEvent =
            remote.updateTimelineEvent(
                timelineEventId = command.timelineEventId,
                request = command.toRequestJson(),
            ).toDomain()
    }
