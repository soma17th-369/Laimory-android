package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.TimelineRecordRemoteDataSource
import com.soma369.laimory.core.data.model.timeline.response.TimelineEventResponse
import com.soma369.laimory.core.data.model.timeline.response.TimelineItemResponse
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.TimelineItemType
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class TimelineRecordRepositoryImplTest {
    private class FakeRemote : TimelineRecordRemoteDataSource {
        var requestedEventId: Long? = null
        var requestedBody: JsonObject? = null

        override suspend fun updateTimelineEvent(
            timelineEventId: Long,
            request: JsonObject,
        ): TimelineEventResponse {
            requestedEventId = timelineEventId
            requestedBody = request
            return response
        }
    }

    @Test
    fun `updateEvent - 요청을 전달하고 PHOTO가 추가된 응답을 Domain으로 매핑한다`() =
        runTest {
            val remote = FakeRemote()
            val repository = TimelineRecordRepositoryImpl(remote)
            val command =
                UpdateTimelineEventCommand(
                    timelineEventId = 17L,
                    title = "수정 제목",
                    subtitle = null,
                    startAt = LocalDateTime.of(2026, 7, 8, 14, 0),
                    endAt = null,
                    eventType = TimelineEventType.PHOTO_MOMENT,
                )

            val event = repository.updateEvent(command)

            assertEquals(17L, remote.requestedEventId)
            assertEquals("수정 제목", remote.requestedBody?.get("title").toString().trim('"'))
            assertEquals("null", remote.requestedBody?.get("subtitle").toString())
            assertEquals(TimelineEventType.PHOTO_MOMENT, event.eventType)
            assertEquals(TimelineItemType.PHOTO, event.items.single().itemType)
            assertNull(event.items.single().startAt)
            assertEquals("https://cdn/photo.jpg", event.items.single().photoUrl)
        }

    private companion object {
        val response =
            TimelineEventResponse(
                timelineEventId = 17L,
                eventType = "PHOTO_MOMENT",
                startAt = "2026-07-08T14:00:00",
                endAt = null,
                title = "수정 제목",
                subtitle = null,
                memo = null,
                items =
                    listOf(
                        TimelineItemResponse(
                            timelineItemId = 31L,
                            itemType = "PHOTO",
                            rawId = "raw-photo-1",
                            startAt = null,
                            endAt = null,
                            payload = buildJsonObject { put("photoUrl", "https://cdn/photo.jpg") },
                        ),
                    ),
            )
    }
}
