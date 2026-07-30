package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.PhotoCandidate
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.source.PhotoSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class PhotoSelectionUseCasesTest {
    private val window =
        RecordDateWindow(
            start = Instant.parse("2026-07-30T00:00:00Z"),
            end = Instant.parse("2026-07-31T03:00:00Z"),
        )

    @Test
    fun `사진 범위 조회는 사용자 지정 RecordDateWindow를 그대로 전달한다`() =
        runBlocking {
            val source = FakePhotoSource()
            val expected =
                listOf(
                    PhotoCandidate(
                        id = 1L,
                        contentUri = "content://photo/1",
                        takenAt = window.start.plusSeconds(1),
                    ),
                )
            source.candidates = expected

            val actual = GetPhotosInWindowUseCase(source)(window)

            assertEquals(window, source.requestedWindow)
            assertEquals(expected, actual)
        }

    @Test
    fun `선택 사진 변환은 저장하지 않고 접근 불가 MediaStore ID를 구분한다`() =
        runBlocking {
            val source = FakePhotoSource()
            source.collectedItems = listOf(sourceItem(mediaStoreId = 2L))

            val result = PrepareSelectedPhotosUseCase(source)(listOf(1L, 2L, 3L))

            assertEquals(listOf(1L, 2L, 3L), source.collectedIds)
            assertEquals(listOf("2"), result.items.map(SourceItem::sourceKey))
            assertEquals(setOf(1L, 3L), result.unavailableIds)
        }

    private fun sourceItem(mediaStoreId: Long): SourceItem =
        SourceItem(
            rawId = "photo-$mediaStoreId",
            startAt = window.start,
            endAt = null,
            timeZoneId = ZoneId.of("UTC"),
            payload = PhotoPayload("photo.jpg", "content://photo/$mediaStoreId", null, null, null),
            sourceName = SourceName.MEDIA_STORE,
            sourceKey = mediaStoreId.toString(),
            collectedAt = window.start,
        )

    private class FakePhotoSource : PhotoSource {
        var candidates: List<PhotoCandidate> = emptyList()
        var collectedItems: List<SourceItem> = emptyList()
        var requestedWindow: RecordDateWindow? = null
        var collectedIds: List<Long> = emptyList()

        override suspend fun photosIn(window: RecordDateWindow): List<PhotoCandidate> {
            requestedWindow = window
            return candidates
        }

        override suspend fun collect(ids: List<Long>): List<SourceItem> {
            collectedIds = ids
            return collectedItems
        }
    }
}
