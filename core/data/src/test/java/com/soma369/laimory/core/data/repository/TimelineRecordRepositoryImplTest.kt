package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.data.datasource.remote.TimelineRecordRemoteDataSource
import com.soma369.laimory.core.data.model.timeline.response.DailyTimelineListResponse
import com.soma369.laimory.core.data.model.timeline.response.DailyTimelineResponse
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TimelineRecordRepositoryImplTest {
    private class FakeRemote : TimelineRecordRemoteDataSource {
        var requestedRecordDate: LocalDate? = null
        var requestedEventId: Long? = null
        var requestedEventFetchId: Long? = null
        var requestedBody: JsonObject? = null
        var deletedEventId: Long? = null
        var deletedRecordDate: LocalDate? = null
        var updateFailure: Throwable? = null
        var getEventFailure: Throwable? = null
        val calls = mutableListOf<String>()

        override suspend fun getDailyRecords(): DailyTimelineListResponse = listResponse

        override suspend fun getDailyRecord(recordDate: LocalDate): DailyTimelineResponse {
            requestedRecordDate = recordDate
            return dailyResponse
        }

        override suspend fun getTimelineEvent(timelineEventId: Long): TimelineEventResponse {
            calls += "GET"
            requestedEventFetchId = timelineEventId
            getEventFailure?.let { throw it }
            return response
        }

        override suspend fun updateTimelineEvent(
            timelineEventId: Long,
            request: JsonObject,
        ) {
            calls += "PATCH"
            requestedEventId = timelineEventId
            requestedBody = request
            updateFailure?.let { throw it }
        }

        override suspend fun deleteTimelineEvent(timelineEventId: Long) {
            deletedEventId = timelineEventId
        }

        override suspend fun deleteDailyRecord(recordDate: LocalDate) {
            deletedRecordDate = recordDate
        }
    }

    @Test
    fun `getDailyRecords - 서버 순서를 보존해 Domain 목록으로 매핑한다`() =
        runTest {
            val remote = FakeRemote()
            val repository = TimelineRecordRepositoryImpl(remote)

            val timelines = repository.getDailyRecords()

            assertEquals(listOf(32L, 31L), timelines.map { it.dailyRecordId })
            assertEquals(LocalDate.of(2026, 7, 28), timelines.first().recordDate)
            assertNull(timelines.first().emotion)
            assertTrue(timelines.first().events.isEmpty())
        }

    @Test
    fun `getDailyRecord - recordDate를 전달하고 graph를 Domain으로 매핑한다`() =
        runTest {
            val remote = FakeRemote()
            val repository = TimelineRecordRepositoryImpl(remote)

            val timeline = repository.getDailyRecord(RECORD_DATE)

            assertEquals(RECORD_DATE, remote.requestedRecordDate)
            assertEquals(31L, timeline.dailyRecordId)
            assertEquals(LocalDate.of(2026, 7, 27), timeline.recordDate)
            val item = timeline.events.single().items.single()
            assertEquals(TimelineItemType.PHOTO, item.itemType)
            assertEquals("https://cdn/photo.jpg", item.photoUrl)
        }

    @Test
    fun `updateEvent - PATCH 후 GET한 최신 Event를 Domain으로 매핑한다`() =
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
            assertEquals(17L, remote.requestedEventFetchId)
            assertEquals(listOf("PATCH", "GET"), remote.calls)
            assertEquals("수정 제목", remote.requestedBody?.get("title").toString().trim('"'))
            assertEquals("null", remote.requestedBody?.get("subtitle").toString())
            assertEquals(TimelineEventType.PHOTO_MOMENT, event.eventType)
            assertEquals(TimelineItemType.PHOTO, event.items.single().itemType)
            assertNull(event.items.single().startAt)
            assertEquals("https://cdn/photo.jpg", event.items.single().photoUrl)
        }

    @Test
    fun `updateEvent - PATCH가 실패하면 GET을 호출하지 않고 오류를 전파한다`() =
        runTest {
            val remote = FakeRemote()
            val repository = TimelineRecordRepositoryImpl(remote)
            remote.updateFailure = IllegalStateException("PATCH 실패")
            val command =
                UpdateTimelineEventCommand(
                    timelineEventId = 17L,
                    title = "수정 제목",
                    subtitle = null,
                    startAt = LocalDateTime.of(2026, 7, 8, 14, 0),
                    endAt = null,
                    eventType = TimelineEventType.PHOTO_MOMENT,
                )

            val failure = runCatching { repository.updateEvent(command) }.exceptionOrNull()

            assertTrue(failure is IllegalStateException)
            assertFalse(remote.calls.contains("GET"))
            assertNull(remote.requestedEventFetchId)
        }

    @Test
    fun `updateEvent - PATCH 성공 후 GET이 실패하면 오류를 전파한다`() =
        runTest {
            val remote = FakeRemote()
            val repository = TimelineRecordRepositoryImpl(remote)
            remote.getEventFailure = IllegalStateException("GET 실패")
            val command =
                UpdateTimelineEventCommand(
                    timelineEventId = 17L,
                    title = "수정 제목",
                    subtitle = null,
                    startAt = LocalDateTime.of(2026, 7, 8, 14, 0),
                    endAt = null,
                    eventType = TimelineEventType.PHOTO_MOMENT,
                )

            val failure = runCatching { repository.updateEvent(command) }.exceptionOrNull()

            assertTrue(failure is IllegalStateException)
            assertEquals(listOf("PATCH", "GET"), remote.calls)
            assertEquals(17L, remote.requestedEventFetchId)
        }

    @Test
    fun `삭제 요청은 Event와 DailyRecord 경로를 구분해 전달한다`() =
        runTest {
            val remote = FakeRemote()
            val repository = TimelineRecordRepositoryImpl(remote)

            repository.deleteEvent(17L)
            repository.deleteDailyRecord(RECORD_DATE)

            assertEquals(17L, remote.deletedEventId)
            assertEquals(RECORD_DATE, remote.deletedRecordDate)
        }

    private companion object {
        val RECORD_DATE: LocalDate = LocalDate.of(2026, 7, 27)

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

        val dailyResponse =
            DailyTimelineResponse(
                dailyRecordId = 31L,
                recordDate = "2026-07-27",
                emotionType = "HAPPY",
                events = listOf(response),
            )

        val listResponse =
            DailyTimelineListResponse(
                timelines =
                    listOf(
                        DailyTimelineResponse(
                            dailyRecordId = 32L,
                            recordDate = "2026-07-28",
                            emotionType = null,
                            events = emptyList(),
                        ),
                        dailyResponse,
                    ),
            )
    }
}
