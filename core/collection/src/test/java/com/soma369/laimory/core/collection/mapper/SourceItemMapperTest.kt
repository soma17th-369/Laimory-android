package com.soma369.laimory.core.collection.mapper

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.collection.StayPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class SourceItemMapperTest {
    @Test
    fun `SourceItem 은 entity 로 변환 후 다시 domain 으로 복원된다`() {
        val item =
            SourceItem(
                rawId = "019f18c2-fde0-7a3c-b1e4-5f9c6d2e8a70",
                startAt = Instant.parse("2026-06-30T01:00:00Z"),
                endAt = Instant.parse("2026-06-30T04:31:04Z"),
                timeZoneId = ZoneId.of("Asia/Seoul"),
                payload = StayPayload(latitude = 37.1538856, longitude = 127.0781832),
                sourceName = SourceName.LOCATION_PROVIDER,
                sourceKey = "2026-06-30T01:00:00Z",
                collectedAt = Instant.parse("2026-06-30T13:41:00Z"),
            )

        assertEquals(item, item.toEntity().toDomain())
    }

    @Test
    fun `단일 시점 이벤트의 endAt null 은 보존된다`() {
        val item =
            SourceItem(
                rawId = "019f18c5-0562-7b18-93ae-4f1d7c2b6083",
                startAt = Instant.parse("2026-06-30T12:57:46Z"),
                endAt = null,
                timeZoneId = ZoneId.of("Asia/Seoul"),
                payload =
                    PhotoPayload(
                        fileName = "0197b1c2.jpg",
                        clientPhotoUri = "content://media/external/images/media/1000010519",
                        latitude = null,
                        longitude = null,
                        description = null,
                    ),
                sourceName = SourceName.MEDIA_STORE,
                sourceKey = "1000010519",
                collectedAt = Instant.parse("2026-06-30T13:41:00Z"),
            )

        val restored = item.toEntity().toDomain()

        assertNull(restored.endAt)
        assertEquals(item, restored)
    }

    @Test
    fun `entity 의 itemType 문자열은 payload 타입에서 파생된다`() {
        val item =
            SourceItem(
                rawId = "raw-id",
                startAt = Instant.parse("2026-06-30T01:00:00Z"),
                endAt = null,
                timeZoneId = ZoneId.of("Asia/Seoul"),
                payload = StayPayload(latitude = 0.0, longitude = 0.0),
                sourceName = SourceName.LOCATION_PROVIDER,
                sourceKey = "source-key",
                collectedAt = Instant.parse("2026-06-30T01:00:00Z"),
            )

        assertEquals(ItemType.STAY.name, item.toEntity().itemType)
    }
}
