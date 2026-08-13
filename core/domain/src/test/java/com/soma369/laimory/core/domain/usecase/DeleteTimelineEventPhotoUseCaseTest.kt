package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.exception.TimelineEventPhotoDeleteException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.model.timeline.TimelineItem
import com.soma369.laimory.core.domain.model.timeline.TimelineItemType
import com.soma369.laimory.core.domain.model.timeline.UpdateTimelineEventCommand
import com.soma369.laimory.core.domain.repository.TimelineRecordRepository
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class DeleteTimelineEventPhotoUseCaseTest {
    @Test
    fun `삭제 성공 뒤 대상 Item만 세션에서 제거한다`() =
        runBlocking {
            val repository = FakeRecordRepository()
            val session = FakeSessionRepository(timeline())
            val useCase = DeleteTimelineEventPhotoUseCase(repository, session, RecordingMessageHelper())

            val result = useCase(EVENT_ID, PHOTO_ITEM_ID)

            assertEquals(DeleteTimelineEventPhotoOutcome.Deleted, result.getOrNull())
            assertEquals(EVENT_ID to PHOTO_ITEM_ID, repository.deletedPhotoIds)
            assertEquals(listOf(OTHER_ITEM_ID), session.eventItems().map(TimelineItem::timelineItemId))
        }

    @Test
    fun `삭제 404는 DailyRecord를 재조회해 최신 세션으로 교체한다`() =
        runBlocking {
            val latestTimeline = timeline(items = listOf(item(OTHER_ITEM_ID)))
            val repository =
                FakeRecordRepository(
                    deletePhotoFailure = ApiException.ClientException(errorCode = -404, rawCode = 404),
                    dailyRecordResult = Result.success(latestTimeline),
                )
            val session = FakeSessionRepository(timeline())
            val useCase = DeleteTimelineEventPhotoUseCase(repository, session, RecordingMessageHelper())

            val result = useCase(EVENT_ID, PHOTO_ITEM_ID)

            assertEquals(DeleteTimelineEventPhotoOutcome.Reconciled, result.getOrNull())
            assertEquals(RECORD_DATE, repository.requestedRecordDate)
            assertEquals(latestTimeline, session.timeline.value)
        }

    @Test
    fun `삭제 404 뒤 DailyRecord도 없으면 세션을 비우고 Event unavailable을 반환한다`() =
        runBlocking {
            val repository =
                FakeRecordRepository(
                    deletePhotoFailure = ApiException.ClientException(errorCode = -404, rawCode = 404),
                    dailyRecordResult =
                        Result.failure(ApiException.ClientException(errorCode = -404, rawCode = 404)),
                )
            val session = FakeSessionRepository(timeline())
            val useCase = DeleteTimelineEventPhotoUseCase(repository, session, RecordingMessageHelper())

            val result = useCase(EVENT_ID, PHOTO_ITEM_ID)

            assertEquals(DeleteTimelineEventPhotoOutcome.EventUnavailable, result.getOrNull())
            assertNull(session.timeline.value)
        }

    @Test
    fun `작성 완료와 PHOTO 타입 불일치는 기능 오류로 변환하고 세션을 유지한다`() =
        runBlocking {
            val cases =
                listOf(
                    ApiException.ConflictException(errorCode = -1003, rawCode = 409) to
                        TimelineEventPhotoDeleteException.Reason.RECORD_ALREADY_SAVED,
                    ApiException.ClientException(errorCode = -1018, rawCode = 400) to
                        TimelineEventPhotoDeleteException.Reason.ITEM_NOT_PHOTO,
                )

            cases.forEach { (apiException, expectedReason) ->
                val session = FakeSessionRepository(timeline())
                val useCase =
                    DeleteTimelineEventPhotoUseCase(
                        FakeRecordRepository(deletePhotoFailure = apiException),
                        session,
                        RecordingMessageHelper(),
                    )

                val failure = useCase(EVENT_ID, PHOTO_ITEM_ID).exceptionOrNull()

                assertTrue(failure is TimelineEventPhotoDeleteException)
                assertEquals(expectedReason, (failure as TimelineEventPhotoDeleteException).reason)
                assertEquals(listOf(PHOTO_ITEM_ID, OTHER_ITEM_ID), session.eventItems().map(TimelineItem::timelineItemId))
            }
        }

    @Test
    fun `401은 BaseUseCase 세션 만료 정책으로 처리한다`() =
        runBlocking {
            val helper = RecordingMessageHelper()
            val useCase =
                DeleteTimelineEventPhotoUseCase(
                    FakeRecordRepository(
                        deletePhotoFailure = ApiException.UnauthorizedException(errorCode = -2001, rawCode = 401),
                    ),
                    FakeSessionRepository(timeline()),
                    helper,
                )

            val failure = useCase(EVENT_ID, PHOTO_ITEM_ID).exceptionOrNull()

            assertTrue(failure is HandledException)
            assertEquals(listOf(UserMessage.SessionExpired), helper.messages)
        }

    private class FakeRecordRepository(
        private val deletePhotoFailure: ApiException? = null,
        private val dailyRecordResult: Result<DailyTimeline>? = null,
    ) : TimelineRecordRepository {
        var deletedPhotoIds: Pair<Long, Long>? = null
        var requestedRecordDate: LocalDate? = null

        override suspend fun getDailyRecords(): List<DailyTimeline> = error("사용하지 않음")

        override suspend fun getDailyRecord(recordDate: LocalDate): DailyTimeline {
            requestedRecordDate = recordDate
            return requireNotNull(dailyRecordResult).getOrThrow()
        }

        override suspend fun updateEvent(command: UpdateTimelineEventCommand): TimelineEvent = error("사용하지 않음")

        override suspend fun updateEventMemo(
            timelineEventId: Long,
            memo: String?,
        ): TimelineEvent = error("사용하지 않음")

        override suspend fun deleteEvent(timelineEventId: Long) = error("사용하지 않음")

        override suspend fun deleteEventPhoto(
            timelineEventId: Long,
            timelineItemId: Long,
        ) {
            deletePhotoFailure?.let { throw it }
            deletedPhotoIds = timelineEventId to timelineItemId
        }

        override suspend fun saveDailyRecord(recordDate: LocalDate) = error("사용하지 않음")

        override suspend fun deleteDailyRecord(recordDate: LocalDate) = error("사용하지 않음")
    }

    private class FakeSessionRepository(initial: DailyTimeline?) : TimelineRecordSessionRepository {
        private val mutableTimeline = MutableStateFlow(initial)
        override val timeline: StateFlow<DailyTimeline?> = mutableTimeline

        override fun save(timeline: DailyTimeline) {
            mutableTimeline.value = timeline
        }

        override fun replaceEvent(event: TimelineEvent) = Unit

        override fun removeEvent(timelineEventId: Long) = Unit

        override fun removeEventItem(
            timelineEventId: Long,
            timelineItemId: Long,
        ) {
            mutableTimeline.value =
                mutableTimeline.value?.copy(
                    events =
                        mutableTimeline.value?.events.orEmpty().map { event ->
                            if (event.timelineEventId == timelineEventId) {
                                event.copy(items = event.items.filterNot { it.timelineItemId == timelineItemId })
                            } else {
                                event
                            }
                        },
                )
        }

        override fun clear() {
            mutableTimeline.value = null
        }

        fun eventItems(): List<TimelineItem> = timeline.value?.events?.single()?.items.orEmpty()
    }

    private class RecordingMessageHelper : MessageHelper {
        val messages = mutableListOf<UserMessage>()

        override fun send(message: UserMessage) {
            messages += message
        }
    }

    private fun timeline(items: List<TimelineItem> = listOf(item(PHOTO_ITEM_ID), item(OTHER_ITEM_ID))) =
        DailyTimeline(
            dailyRecordId = 31L,
            recordDate = RECORD_DATE,
            emotion = null,
            events =
                listOf(
                    TimelineEvent(
                        timelineEventId = EVENT_ID,
                        eventType = TimelineEventType.PHOTO_MOMENT,
                        startAt = LocalDateTime.of(2026, 8, 5, 12, 0),
                        endAt = null,
                        title = "사진",
                        subtitle = null,
                        memo = null,
                        items = items,
                    ),
                ),
        )

    private fun item(id: Long) =
        TimelineItem(
            timelineItemId = id,
            itemType = TimelineItemType.PHOTO,
            rawId = "photo-$id",
            startAt = null,
            endAt = null,
            photoUrl = "https://photo/$id.jpg",
        )

    private companion object {
        const val EVENT_ID = 17L
        const val PHOTO_ITEM_ID = 31L
        const val OTHER_ITEM_ID = 32L
        val RECORD_DATE: LocalDate = LocalDate.of(2026, 8, 5)
    }
}
