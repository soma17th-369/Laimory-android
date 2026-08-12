package com.soma369.laimory.core.domain.usecase

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
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PrepareTimelineDraftSelectionUseCaseTest {
    private val zone: ZoneId = ZoneId.of("Asia/Seoul")
    private val date: LocalDate = LocalDate.of(2026, 7, 8)

    private fun useCase(
        selectionPolicy: DraftSourceItemSelectionPolicy = DraftSourceItemSelectionPolicy(),
        selectionReporter: DraftSourceItemSelectionReporter = DraftSourceItemSelectionReporter.NONE,
    ): PrepareTimelineDraftSelectionUseCase = PrepareTimelineDraftSelectionUseCase(selectionPolicy, selectionReporter)

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
    fun `전송 목록을 startAt 오름차순으로 확정한다`() {
        val useCase = useCase()
        val newest = item(at(18))
        val middle = item(at(12))
        val oldest = item(at(9))

        val result = useCase(RecordDateWindow.ofDate(date, zone), listOf(newest, middle, oldest))

        assertEquals(listOf(oldest, middle, newest), result.getOrThrow().items)
    }

    @Test
    fun `기록 창 밖 아이템은 선택과 리포트 원본 건수에서 제외한다`() {
        val useCase = useCase()
        val window =
            RecordDateWindow(
                start = at(9),
                end = date.plusDays(1).atTime(2, 0).atZone(zone).toInstant(),
            )
        val before = item(at(8))
        val inside = item(at(20))

        val selection = useCase(window, listOf(before, inside)).getOrThrow()

        assertEquals(listOf(inside), selection.items)
        assertEquals(1, selection.report.originalTotal)
    }

    @Test
    fun `타입 상한을 적용해 최신 항목만 선택하고 원본·선택 건수를 함께 보고한다`() {
        val useCase =
            useCase(
                selectionPolicy =
                    DraftSourceItemSelectionPolicy(
                        limits = DraftSourceItemLimits(notification = 2),
                    ),
            )
        val oldest = item(at(9), rawId = "oldest")
        val middle = item(at(12), rawId = "middle")
        val newest = item(at(18), rawId = "newest")

        val selection = useCase(RecordDateWindow.ofDate(date, zone), listOf(oldest, newest, middle)).getOrThrow()

        assertEquals(listOf(middle, newest), selection.items)
        assertEquals(3, selection.report.originalTotal)
        assertEquals(2, selection.report.selectedTotal)
    }

    @Test
    fun `PHOTO 상한 초과는 자동 절삭하지 않고 실패한다`() {
        val useCase =
            useCase(
                selectionPolicy =
                    DraftSourceItemSelectionPolicy(
                        limits = DraftSourceItemLimits(photo = 2),
                    ),
            )
        val photos =
            (1..3).map { index ->
                item(
                    start = at(10, index),
                    rawId = "photo-$index",
                    payload = PhotoPayload("$index.jpg", "content://$index", null, null, null),
                )
            }

        val result = useCase(RecordDateWindow.ofDate(date, zone), photos)

        assertTrue(result.exceptionOrNull() is DraftPhotoLimitExceededException)
    }

    @Test
    fun `활성화된 리포터에는 확정된 선택 리포트를 전달한다`() {
        var reported: DraftSourceItemSelectionReport? = null
        val reporter =
            object : DraftSourceItemSelectionReporter {
                override val isEnabled: Boolean = true

                override fun reportSelection(report: DraftSourceItemSelectionReport) {
                    reported = report
                }

                override fun reportRequestSize(
                    sourceItemCount: Int,
                    utf8ByteCount: Int,
                ) = Unit
            }
        val useCase = useCase(selectionReporter = reporter)

        val selection = useCase(RecordDateWindow.ofDate(date, zone), listOf(item(at(9)))).getOrThrow()

        assertEquals(selection.report, reported)
    }

    @Test
    fun `비활성 리포터는 호출하지 않는다`() {
        var reported: DraftSourceItemSelectionReport? = null
        val reporter =
            object : DraftSourceItemSelectionReporter {
                override val isEnabled: Boolean = false

                override fun reportSelection(report: DraftSourceItemSelectionReport) {
                    reported = report
                }

                override fun reportRequestSize(
                    sourceItemCount: Int,
                    utf8ByteCount: Int,
                ) = Unit
            }
        val useCase = useCase(selectionReporter = reporter)

        useCase(RecordDateWindow.ofDate(date, zone), listOf(item(at(9)))).getOrThrow()

        assertNull(reported)
    }

    @Test
    fun `리포터 실패도 Result failure 로 반환한다`() {
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
        val useCase = useCase(selectionReporter = reporter)

        val result = useCase(RecordDateWindow.ofDate(date, zone), listOf(item(at(9))))

        assertEquals(reporterFailure, result.exceptionOrNull())
    }
}
