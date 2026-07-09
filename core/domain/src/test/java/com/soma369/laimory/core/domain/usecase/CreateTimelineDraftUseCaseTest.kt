package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceItemPayload
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.timeline.DraftTaskHandle
import com.soma369.laimory.core.domain.model.timeline.DraftTaskSnapshot
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CreateTimelineDraftUseCaseTest {
    private val zone: ZoneId = ZoneId.of("Asia/Seoul")
    private val date: LocalDate = LocalDate.of(2026, 7, 8)

    /** createDraft 로 전달된 items 를 잡아두는 fake 리포지토리. */
    private class CapturingRepository : TimelineDraftRepository {
        var createdItems: List<SourceItem>? = null
        var uploadedUris: List<String>? = null

        override suspend fun uploadPhotos(clientPhotoUris: List<String>): List<String> {
            uploadedUris = clientPhotoUris
            return clientPhotoUris.map { "uploaded-$it" }
        }

        override suspend fun createDraft(
            recordDate: LocalDate,
            zone: ZoneId,
            items: List<SourceItem>,
            uploadedPhotoFilenames: Map<String, String>,
        ): DraftTaskHandle {
            createdItems = items
            return DraftTaskHandle(taskId = "task-1")
        }

        override suspend fun getDraftStatus(taskId: String): DraftTaskSnapshot = throw UnsupportedOperationException()
    }

    private val noopMessageHelper =
        object : MessageHelper {
            override fun send(message: UserMessage) = Unit
        }

    private fun at(hour: Int): Instant = date.atTime(hour, 0).atZone(zone).toInstant()

    private fun item(
        start: Instant,
        payload: SourceItemPayload =
            NotificationPayload("app", "com.app", "t", "x", NotificationPayload.CollectReason.ALL),
    ): SourceItem =
        SourceItem(
            rawId = "raw-$start",
            startAt = start,
            endAt = null,
            timeZoneId = zone,
            payload = payload,
            sourceName = SourceName.NOTIFICATION_LISTENER,
            sourceKey = "key-$start",
            collectedAt = start,
        )

    @Test
    fun `sourceItems 를 startAt 오름차순으로 정렬해 전송한다`() =
        runBlocking {
            val repo = CapturingRepository()
            val useCase = CreateTimelineDraftUseCase(repo, noopMessageHelper)
            // 표시용 최신순(내림차순)으로 들어와도 전송은 시간순이어야 한다.
            val newest = item(at(18))
            val middle = item(at(12))
            val oldest = item(at(9))

            val result = useCase(date, zone, listOf(newest, middle, oldest))

            assertTrue(result.isSuccess)
            assertEquals(listOf(oldest, middle, newest), repo.createdItems)
        }

    @Test
    fun `PHOTO 파일명 매핑은 정렬 후에도 rawId 기준으로 정확하다`() =
        runBlocking {
            val repo = CapturingRepository()
            val useCase = CreateTimelineDraftUseCase(repo, noopMessageHelper)
            val latePhoto = item(at(20), PhotoPayload("late.jpg", "content://late", null, null, null))
            val earlyPhoto = item(at(8), PhotoPayload("early.jpg", "content://early", null, null, null))

            val result = useCase(date, zone, listOf(latePhoto, earlyPhoto))

            assertTrue(result.isSuccess)
            // 전송 순서는 시간순(early → late), 업로드도 그 순서의 URI 로 나간다.
            assertEquals(listOf(earlyPhoto, latePhoto), repo.createdItems)
            assertEquals(listOf("content://early", "content://late"), repo.uploadedUris)
        }
}
