package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.timeline.request.UpdateTimelineEventMemoRequest
import com.soma369.laimory.core.data.model.timeline.response.DailyTimelineListResponse
import com.soma369.laimory.core.data.model.timeline.response.DailyTimelineResponse
import com.soma369.laimory.core.data.model.timeline.response.TimelineEventResponse
import com.soma369.laimory.core.data.network.api.TimelineRecordApi
import com.soma369.laimory.core.data.network.safeApiCall
import com.soma369.laimory.core.data.network.safeApiCallUnit
import kotlinx.serialization.json.JsonObject
import java.time.LocalDate
import javax.inject.Inject

class TimelineRecordRemoteDataSourceImpl
    @Inject
    constructor(
        private val api: TimelineRecordApi,
    ) : TimelineRecordRemoteDataSource {
        override suspend fun getDailyRecords(): DailyTimelineListResponse = safeApiCall { api.getDailyRecords() }

        override suspend fun getDailyRecord(recordDate: LocalDate): DailyTimelineResponse =
            safeApiCall { api.getDailyRecord(recordDate.toString()) }

        override suspend fun getTimelineEvent(timelineEventId: Long): TimelineEventResponse =
            safeApiCall { api.getTimelineEvent(timelineEventId) }

        override suspend fun updateTimelineEvent(
            timelineEventId: Long,
            request: JsonObject,
        ) {
            safeApiCallUnit { api.updateTimelineEvent(timelineEventId, request) }
        }

        override suspend fun updateTimelineEventMemo(
            timelineEventId: Long,
            request: UpdateTimelineEventMemoRequest,
        ) {
            safeApiCallUnit { api.updateTimelineEventMemo(timelineEventId, request) }
        }

        override suspend fun deleteTimelineEvent(timelineEventId: Long) {
            safeApiCallUnit { api.deleteTimelineEvent(timelineEventId) }
        }

        override suspend fun deleteTimelineEventPhoto(
            timelineEventId: Long,
            timelineItemId: Long,
        ) {
            safeApiCallUnit { api.deleteTimelineEventPhoto(timelineEventId, timelineItemId) }
        }

        override suspend fun deleteDailyRecord(recordDate: LocalDate) {
            safeApiCallUnit { api.deleteDailyRecord(recordDate.toString()) }
        }

        override suspend fun saveDailyRecord(recordDate: LocalDate) {
            safeApiCallUnit { api.saveDailyRecord(recordDate.toString()) }
        }
    }
