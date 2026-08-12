package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceItemPayload
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelection
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionPolicy
import com.soma369.laimory.core.domain.model.timeline.DraftTaskHandle
import com.soma369.laimory.core.domain.model.timeline.DraftTaskSnapshot
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        var createdWindow: RecordDateWindow? = null

        override suspend fun uploadPhotos(clientPhotoUris: List<String>): List<String> {
            uploadedUris = clientPhotoUris
            return clientPhotoUris.map { "uploaded-$it" }
        }

        override suspend fun createDraft(
            recordDate: LocalDate,
            zone: ZoneId,
            window: RecordDateWindow,
            items: List<SourceItem>,
            uploadedPhotoFilenames: Map<String, String>,
        ): DraftTaskHandle {
            createdWindow = window
            createdItems = items
            uploadedFilenamesByRawId = uploadedPhotoFilenames
            return DraftTaskHandle(taskId = "task-1")
        }

        var uploadedFilenamesByRawId: Map<String, String> = emptyMap()

        override suspend fun getDraftStatus(taskId: String): DraftTaskSnapshot = throw UnsupportedOperationException()
    }

    private val noopMessageHelper =
        object : MessageHelper {
            override fun send(message: UserMessage) = Unit
        }

    private fun at(
        hour: Int,
        minute: Int = 0,
    ): Instant = date.atTime(hour, minute).atZone(zone).toInstant()

    private fun item(
        start: Instant,
        payload: SourceItemPayload =
            NotificationPayload("app", "com.app", "t", "x", NotificationPayload.CollectReason.ALL),
        rawId: String = "raw-$start",
    ): SourceItem =
        SourceItem(
            rawId = rawId,
            startAt = start,
            endAt = null,
            timeZoneId = zone,
            payload = payload,
            sourceName = SourceName.NOTIFICATION_LISTENER,
            sourceKey = "key-$start",
            collectedAt = start,
        )

    /** 실제 준비 단계와 같은 정책으로 selection 을 확정한다 — 준비 결과 = 전송 입력 계약을 그대로 검증한다. */
    private fun selectionOf(
        window: RecordDateWindow,
        items: List<SourceItem>,
    ): DraftSourceItemSelection = DraftSourceItemSelectionPolicy().select(window, items).getOrThrow()

    @Test
    fun `확정된 selection 아이템을 순서 그대로 전송한다`() =
        runBlocking {
            val repo = CapturingRepository()
            val useCase = CreateTimelineDraftUseCase(repo, noopMessageHelper)
            val window = RecordDateWindow.ofDate(date, zone)
            val selection = selectionOf(window, listOf(item(at(18)), item(at(12)), item(at(9))))

            val result = useCase(date, zone, window, selection)

            assertTrue(result.isSuccess)
            assertEquals(window, repo.createdWindow)
            assertEquals(selection.items, repo.createdItems)
        }

    @Test
    fun `PHOTO 가 없으면 업로드를 호출하지 않는다`() =
        runBlocking {
            val repo = CapturingRepository()
            val useCase = CreateTimelineDraftUseCase(repo, noopMessageHelper)
            val window = RecordDateWindow.ofDate(date, zone)

            val result = useCase(date, zone, window, selectionOf(window, listOf(item(at(9)))))

            assertTrue(result.isSuccess)
            assertNull(repo.uploadedUris)
        }

    @Test
    fun `PHOTO 파일명 매핑은 정렬 후에도 rawId 기준으로 정확하다`() =
        runBlocking {
            val repo = CapturingRepository()
            val useCase = CreateTimelineDraftUseCase(repo, noopMessageHelper)
            val window = RecordDateWindow.ofDate(date, zone)
            val latePhoto = item(at(20), PhotoPayload("late.jpg", "content://late", null, null, null), rawId = "late")
            val earlyPhoto = item(at(8), PhotoPayload("early.jpg", "content://early", null, null, null), rawId = "early")

            val result = useCase(date, zone, window, selectionOf(window, listOf(latePhoto, earlyPhoto)))

            assertTrue(result.isSuccess)
            // 전송 순서는 시간순(early → late), 업로드도 그 순서의 URI 로 나간다.
            assertEquals(listOf(earlyPhoto, latePhoto), repo.createdItems)
            assertEquals(listOf("content://early", "content://late"), repo.uploadedUris)
            assertEquals(
                mapOf("early" to "uploaded-content://early", "late" to "uploaded-content://late"),
                repo.uploadedFilenamesByRawId,
            )
        }
}
