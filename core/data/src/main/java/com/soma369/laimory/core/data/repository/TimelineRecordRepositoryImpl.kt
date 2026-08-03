package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.TimelineRecordRemoteDataSource
import com.soma369.laimory.core.data.model.timeline.request.toRequestJson
import com.soma369.laimory.core.data.model.timeline.response.toDomain
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import javax.inject.Inject

class TimelineRecordRepositoryImpl
    @Inject
    constructor(
        private val remote: TimelineRecordRemoteDataSource,
    ) : TimelineRecordRepository {
        override suspend fun getDailyRecords(): List<DailyTimeline> = remote.getDailyRecords().toDomain()

        override suspend fun getDailyRecord(dailyRecordId: Long): DailyTimeline = remote.getDailyRecord(dailyRecordId).toDomain()

        /** PATCH 성공 응답의 body가 null이므로 서버에 저장된 최신 Event를 GET으로 재조회한다. */
        override suspend fun updateEvent(command: UpdateTimelineEventCommand): TimelineEvent {
            remote.updateTimelineEvent(
                timelineEventId = command.timelineEventId,
                request = command.toRequestJson(),
            )
            return remote.getTimelineEvent(command.timelineEventId).toDomain()
        }

        override suspend fun deleteEvent(timelineEventId: Long) {
            remote.deleteTimelineEvent(timelineEventId)
        }

        override suspend fun deleteDailyRecord(dailyRecordId: Long) {
            remote.deleteDailyRecord(dailyRecordId)
        }
    }
