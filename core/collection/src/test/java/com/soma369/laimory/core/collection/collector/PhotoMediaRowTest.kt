package com.soma369.laimory.core.collection.collector

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class PhotoMediaRowTest {
    private val collectedAt = Instant.parse("2026-07-07T04:00:00Z")
    private val zoneId = ZoneId.of("Asia/Seoul")

    private fun row(
        id: Long = 1000010519L,
        displayName: String = "photo.jpg",
        clientPhotoUri: String = "content://media/external/images/media/1000010519",
        dateTakenMillis: Long? = null,
        dateAddedSeconds: Long? = null,
        latitude: Double? = null,
        longitude: Double? = null,
    ) = PhotoMediaRow(
        id = id,
        displayName = displayName,
        clientPhotoUri = clientPhotoUri,
        dateTakenMillis = dateTakenMillis,
        dateAddedSeconds = dateAddedSeconds,
        latitude = latitude,
        longitude = longitude,
    )

    @Test
    fun `DATE_TAKEN 이 있으면 startAt 은 DATE_TAKEN millis 를 쓴다`() {
        val taken = Instant.parse("2026-06-30T21:57:46Z")
        val item =
            row(dateTakenMillis = taken.toEpochMilli(), dateAddedSeconds = 1_700_000_000L).toSourceItem(
                rawId = "raw-1",
                collectedAt = collectedAt,
                zoneId = zoneId,
            )

        assertEquals(taken, item?.startAt)
    }

    @Test
    fun `DATE_TAKEN 이 0 이면 DATE_ADDED seconds 를 millis 로 환산해 fallback 한다`() {
        val addedSeconds = 1_751_256_000L
        val item =
            row(dateTakenMillis = 0L, dateAddedSeconds = addedSeconds).toSourceItem(
                rawId = "raw-1",
                collectedAt = collectedAt,
                zoneId = zoneId,
            )

        assertEquals(Instant.ofEpochMilli(addedSeconds * 1_000L), item?.startAt)
    }

    @Test
    fun `DATE_TAKEN 이 null 이어도 DATE_ADDED 로 fallback 한다`() {
        val addedSeconds = 1_751_256_000L
        val item =
            row(dateTakenMillis = null, dateAddedSeconds = addedSeconds).toSourceItem(
                rawId = "raw-1",
                collectedAt = collectedAt,
                zoneId = zoneId,
            )

        assertEquals(Instant.ofEpochMilli(addedSeconds * 1_000L), item?.startAt)
    }

    @Test
    fun `유효한 시각이 없으면 null 을 반환해 저장에서 제외한다`() {
        assertNull(
            row(dateTakenMillis = 0L, dateAddedSeconds = 0L).toSourceItem(
                rawId = "raw-1",
                collectedAt = collectedAt,
                zoneId = zoneId,
            ),
        )
        assertNull(
            row(dateTakenMillis = null, dateAddedSeconds = null).toSourceItem(
                rawId = "raw-1",
                collectedAt = collectedAt,
                zoneId = zoneId,
            ),
        )
    }

    @Test
    fun `sourceKey 는 MediaStore _ID 문자열이고 sourceName 은 MEDIA_STORE 다`() {
        val item =
            row(id = 42L, dateTakenMillis = 1L).toSourceItem(
                rawId = "raw-1",
                collectedAt = collectedAt,
                zoneId = zoneId,
            )

        assertEquals("42", item?.sourceKey)
        assertEquals(SourceName.MEDIA_STORE, item?.sourceName)
        assertEquals(ItemType.PHOTO, item?.itemType)
    }

    @Test
    fun `payload 는 filename clientPhotoUri EXIF 위치를 담고 description 은 null 이다`() {
        val item =
            row(
                displayName = "0197b1c2.jpg",
                clientPhotoUri = "content://media/external/images/media/1000010519",
                dateTakenMillis = 1L,
                latitude = 37.5665,
                longitude = 126.9780,
            ).toSourceItem(
                rawId = "raw-1",
                collectedAt = collectedAt,
                zoneId = zoneId,
            )

        val payload = item?.payload as PhotoPayload
        assertEquals("0197b1c2.jpg", payload.fileName)
        assertEquals("content://media/external/images/media/1000010519", payload.clientPhotoUri)
        assertEquals(37.5665, payload.latitude)
        assertEquals(126.9780, payload.longitude)
        assertNull(payload.description)
    }

    @Test
    fun `EXIF 위치가 없으면 payload 의 위경도는 null 이다`() {
        val item =
            row(dateTakenMillis = 1L, latitude = null, longitude = null).toSourceItem(
                rawId = "raw-1",
                collectedAt = collectedAt,
                zoneId = zoneId,
            )

        val payload = item?.payload as PhotoPayload
        assertNull(payload.latitude)
        assertNull(payload.longitude)
    }

    @Test
    fun `endAt 은 단일 시점 이벤트라 null 이고 collectedAt timeZoneId 는 그대로 전달된다`() {
        val item =
            row(dateTakenMillis = 1L).toSourceItem(
                rawId = "raw-1",
                collectedAt = collectedAt,
                zoneId = zoneId,
            )

        assertNull(item?.endAt)
        assertEquals(collectedAt, item?.collectedAt)
        assertEquals(zoneId, item?.timeZoneId)
        assertEquals("raw-1", item?.rawId)
    }
}
