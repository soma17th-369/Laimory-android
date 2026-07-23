package com.soma369.laimory.core.data.datasource.remote

import com.soma369.laimory.core.data.model.timeline.response.TimelineEventResponse
import com.soma369.laimory.core.data.network.api.TimelineRecordApi
import com.soma369.laimory.core.data.network.safeApiCall
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

class TimelineRecordRemoteDataSourceImpl
    @Inject
    constructor(
        private val api: TimelineRecordApi,
    ) : TimelineRecordRemoteDataSource {
        override suspend fun updateTimelineEvent(
            timelineEventId: Long,
            request: JsonObject,
        ): TimelineEventResponse = safeApiCall { api.updateTimelineEvent(timelineEventId, request) }
    }
