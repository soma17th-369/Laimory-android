package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.timeline.response.DailyTimelineListResponse
import com.soma369.laimory.core.data.model.timeline.response.DailyTimelineResponse
import com.soma369.laimory.core.data.model.timeline.response.TimelineEventResponse
import com.soma369.laimory.core.data.network.api.TimelineRecordApi
import com.soma369.laimory.core.data.network.safeApiCall
import com.soma369.laimory.core.data.network.safeApiCallUnit
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

class TimelineRecordRemoteDataSourceImpl
    @Inject
    constructor(
        private val api: TimelineRecordApi,
    ) : TimelineRecordRemoteDataSource {
        override suspend fun getDailyRecords(): DailyTimelineListResponse = safeApiCall { api.getDailyRecords() }

        override suspend fun getDailyRecord(dailyRecordId: Long): DailyTimelineResponse = safeApiCall { api.getDailyRecord(dailyRecordId) }

        override suspend fun updateTimelineEvent(
            timelineEventId: Long,
            request: JsonObject,
        ): TimelineEventResponse = safeApiCall { api.updateTimelineEvent(timelineEventId, request) }

        override suspend fun deleteTimelineEvent(timelineEventId: Long) {
            safeApiCallUnit { api.deleteTimelineEvent(timelineEventId) }
        }

        override suspend fun deleteDailyRecord(dailyRecordId: Long) {
            safeApiCallUnit { api.deleteDailyRecord(dailyRecordId) }
        }
    }
