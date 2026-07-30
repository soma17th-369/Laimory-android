package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceItemPayload
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.timeline.DraftPhotoLimitExceededException
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemLimits
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionPolicy
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionReport
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionReporter
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
            return DraftTaskHandle(taskId = "task-1")
        }

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

    @Test
    fun `sourceItems 를 startAt 오름차순으로 정렬해 전송한다`() =
        runBlocking {
            val repo = CapturingRepository()
            val useCase = CreateTimelineDraftUseCase(repo, noopMessageHelper)
            // 표시용 최신순(내림차순)으로 들어와도 전송은 시간순이어야 한다.
            val newest = item(at(18))
            val middle = item(at(12))
            val oldest = item(at(9))

            val window = RecordDateWindow.ofDate(date, zone)
            val result = useCase(date, zone, window, listOf(newest, middle, oldest))

            assertTrue(result.isSuccess)
            assertEquals(window, repo.createdWindow)
            assertEquals(listOf(oldest, middle, newest), repo.createdItems)
        }

    @Test
    fun `PHOTO 파일명 매핑은 정렬 후에도 rawId 기준으로 정확하다`() =
        runBlocking {
            val repo = CapturingRepository()
            val useCase = CreateTimelineDraftUseCase(repo, noopMessageHelper)
            val latePhoto = item(at(20), PhotoPayload("late.jpg", "content://late", null, null, null))
            val earlyPhoto = item(at(8), PhotoPayload("early.jpg", "content://early", null, null, null))

            val result = useCase(date, zone, RecordDateWindow.ofDate(date, zone), listOf(latePhoto, earlyPhoto))

            assertTrue(result.isSuccess)
            // 전송 순서는 시간순(early → late), 업로드도 그 순서의 URI 로 나간다.
            assertEquals(listOf(earlyPhoto, latePhoto), repo.createdItems)
            assertEquals(listOf("content://early", "content://late"), repo.uploadedUris)
        }

    @Test
    fun `기록 창 밖 아이템은 사진 업로드와 생성 요청에서 모두 제외한다`() =
        runBlocking {
            val repo = CapturingRepository()
            val useCase = CreateTimelineDraftUseCase(repo, noopMessageHelper)
            val window =
                RecordDateWindow(
                    start = at(9),
                    end = date.plusDays(1).atTime(2, 0).atZone(zone).toInstant(),
                )
            val before = item(at(8), PhotoPayload("before.jpg", "content://before", null, null, null))
            val inside = item(at(20), PhotoPayload("inside.jpg", "content://inside", null, null, null))
            val nextDayInside =
                item(
                    date.plusDays(1).atTime(1, 0).atZone(zone).toInstant(),
                    PhotoPayload("next.jpg", "content://next", null, null, null),
                )
            val after =
                item(
                    date.plusDays(1).atTime(3, 0).atZone(zone).toInstant(),
                    PhotoPayload("after.jpg", "content://after", null, null, null),
                )

            val result = useCase(date, zone, window, listOf(after, nextDayInside, inside, before))

            assertTrue(result.isSuccess)
            assertEquals(listOf(inside, nextDayInside), repo.createdItems)
            assertEquals(listOf("content://inside", "content://next"), repo.uploadedUris)
        }

    @Test
    fun `타입 상한을 적용한 최신 항목만 생성 요청에 전달한다`() =
        runBlocking {
            val repo = CapturingRepository()
            val policy =
                DraftSourceItemSelectionPolicy(
                    limits =
                        DraftSourceItemLimits(
                            notification = 2,
                            photo = 10,
                            total = 10,
                        ),
                )
            val useCase =
                CreateTimelineDraftUseCase(
                    repository = repo,
                    messageHelper = noopMessageHelper,
                    selectionPolicy = policy,
                )
            val oldest = item(at(9), rawId = "oldest")
            val middle = item(at(12), rawId = "middle")
            val newest = item(at(18), rawId = "newest")

            val result =
                useCase(
                    recordDate = date,
                    zone = zone,
                    window = RecordDateWindow.ofDate(date, zone),
                    items = listOf(oldest, newest, middle),
                )

            assertTrue(result.isSuccess)
            assertEquals(listOf(middle, newest), repo.createdItems)
        }

    @Test
    fun `PHOTO 상한 초과 시 업로드와 생성 요청을 시작하지 않는다`() =
        runBlocking {
            val repo = CapturingRepository()
            val policy =
                DraftSourceItemSelectionPolicy(
                    limits =
                        DraftSourceItemLimits(
                            photo = 2,
                            total = 10,
                        ),
                )
            val useCase =
                CreateTimelineDraftUseCase(
                    repository = repo,
                    messageHelper = noopMessageHelper,
                    selectionPolicy = policy,
                )
            val photos =
                (1..3).map { index ->
                    item(
                        start = at(10, index),
                        rawId = "photo-$index",
                        payload = PhotoPayload("$index.jpg", "content://$index", null, null, null),
                    )
                }

            val result = useCase(date, zone, RecordDateWindow.ofDate(date, zone), photos)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is DraftPhotoLimitExceededException)
            assertNull(repo.uploadedUris)
            assertNull(repo.createdItems)
        }

    @Test
    fun `선택 리포터 실패도 Result failure로 반환하고 생성 요청을 시작하지 않는다`() =
        runBlocking {
            val repo = CapturingRepository()
            val reporterFailure = IllegalStateException("report failed")
            val reporter =
                object : DraftSourceItemSelectionReporter {
                    override val isEnabled: Boolean = true

                    override fun reportSelection(report: DraftSourceItemSelectionReport) {
                        throw reporterFailure
                    }

                    override fun reportRequestSize(
                        sourceItemCount: Int,
                        utf8ByteCount: Int,
                    ) = Unit
                }
            val useCase =
                CreateTimelineDraftUseCase(
                    repository = repo,
                    messageHelper = noopMessageHelper,
                    selectionReporter = reporter,
                )

            val result =
                useCase(
                    recordDate = date,
                    zone = zone,
                    window = RecordDateWindow.ofDate(date, zone),
                    items = listOf(item(at(9))),
                )

            assertTrue(result.isFailure)
            assertEquals(reporterFailure, result.exceptionOrNull())
            assertNull(repo.uploadedUris)
            assertNull(repo.createdItems)
        }
}
