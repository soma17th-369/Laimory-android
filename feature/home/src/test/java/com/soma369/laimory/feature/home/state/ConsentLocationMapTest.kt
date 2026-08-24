package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.collection.StayPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ConsentLocationMapTest {
    // --- 마커 변환 ---

    @Test
    fun `체류는 마커 한 개 이동은 시작과 도착 두 개로 옮긴다`() {
        val markers = listOf(stay(), movement()).toLocationMarkers(ZONE)

        assertEquals(3, markers.size)
        assertEquals(listOf("stay-1", "move-1:start", "move-1:end"), markers.map { it.key })
    }

    @Test
    fun `이동의 두 마커는 같은 rawId 를 가리켜 전송 포함이 함께 움직인다`() {
        val markers = listOf(movement()).toLocationMarkers(ZONE)

        assertEquals(setOf("move-1"), markers.mapTo(mutableSetOf()) { it.sourceRawId })
    }

    @Test
    fun `마커 제목은 주소를 쓰고 주소가 없으면 대체 문구를 쓴다`() {
        val resolved = listOf(stay(address = "서울특별시 중구 세종대로 110")).toLocationMarkers(ZONE)
        val unresolved = listOf(stay(address = null)).toLocationMarkers(ZONE)

        assertEquals("서울특별시 중구 세종대로 110", resolved.single().title)
        assertEquals("주소 미확인", unresolved.single().title)
    }

    @Test
    fun `마커 번호는 시간순으로 이어지고 이동은 시작과 도착이 각각 받는다`() {
        // 같은 번호를 두 핀이 나눠 가지면 지도에서 어느 쪽이 출발인지 알 수 없다.
        val markers = listOf(stay(), movement()).toLocationMarkers(ZONE)

        assertEquals(listOf(1, 2, 3), markers.map { it.order })
    }

    @Test
    fun `체류와 이동 시작 도착을 종류로 구분한다`() {
        val markers = listOf(stay(), movement()).toLocationMarkers(ZONE)

        assertEquals(
            listOf(
                ConsentLocationMarker.Kind.STAY,
                ConsentLocationMarker.Kind.MOVEMENT_START,
                ConsentLocationMarker.Kind.MOVEMENT_END,
            ),
            markers.map { it.kind },
        )
    }

    @Test
    fun `위치가 아닌 항목은 번호를 소비하지 않는다`() {
        // 알림이 섞여도 지도 번호와 목록 번호가 어긋나면 안 된다.
        val markers = listOf(notification(), stay(), notification(), movement()).toLocationMarkers(ZONE)

        assertEquals(listOf(1, 2, 3), markers.map { it.order })
    }

    @Test
    fun `설명으로 이동 시작과 도착을 구분한다`() {
        val markers = listOf(movement()).toLocationMarkers(ZONE)

        assertTrue(markers[0].snippet.orEmpty().startsWith("이동 시작"))
        assertTrue(markers[1].snippet.orEmpty().startsWith("이동 도착"))
    }

    @Test
    fun `위치가 아닌 항목은 마커로 옮기지 않는다`() {
        val markers = listOf(notification(), stay()).toLocationMarkers(ZONE)

        assertEquals(listOf("stay-1"), markers.map { it.key })
    }

    // --- 카메라 영역 ---

    @Test
    fun `마커가 없으면 카메라 영역도 없다`() {
        assertNull(emptyList<ConsentLocationMarker>().toBounds())
    }

    @Test
    fun `모든 마커를 담는 영역을 만든다`() {
        val bounds = listOf(marker(37.55, 126.97), marker(37.57, 126.99)).toBounds()!!

        assertTrue(bounds.southLatitude <= 37.55)
        assertTrue(bounds.northLatitude >= 37.57)
        assertTrue(bounds.westLongitude <= 126.97)
        assertTrue(bounds.eastLongitude >= 126.99)
    }

    @Test
    fun `마커가 하나뿐이어도 영역이 납작해지지 않는다`() {
        // 넓이가 0이면 지도가 최대 배율까지 확대돼 어디인지 알아보기 어렵다.
        val bounds = listOf(marker(37.5665, 126.9780)).toBounds()!!

        assertTrue(bounds.northLatitude > bounds.southLatitude)
        assertTrue(bounds.eastLongitude > bounds.westLongitude)
    }

    @Test
    fun `같은 자리를 오간 이동도 영역이 납작해지지 않는다`() {
        val bounds = listOf(marker(37.5665, 126.9780), marker(37.5665, 126.9780)).toBounds()!!

        assertTrue(bounds.northLatitude > bounds.southLatitude)
        assertTrue(bounds.eastLongitude > bounds.westLongitude)
    }

    @Test
    fun `이미 충분히 넓은 영역에는 여백을 더하지 않는다`() {
        val bounds = listOf(marker(37.0, 126.0), marker(38.0, 127.0)).toBounds()!!

        assertEquals(37.0, bounds.southLatitude, 1e-9)
        assertEquals(38.0, bounds.northLatitude, 1e-9)
        assertEquals(126.0, bounds.westLongitude, 1e-9)
        assertEquals(127.0, bounds.eastLongitude, 1e-9)
    }

    @Test
    fun `영역의 남서 좌표는 북동 좌표를 넘지 않는다`() {
        val bounds = listOf(marker(37.57, 126.99), marker(37.55, 126.97)).toBounds()!!

        assertTrue(bounds.southLatitude <= bounds.northLatitude)
        assertTrue(bounds.westLongitude <= bounds.eastLongitude)
    }

    private fun marker(
        latitude: Double,
        longitude: Double,
    ) = ConsentLocationMarker(
        key = "$latitude:$longitude",
        sourceRawId = "raw",
        order = 1,
        kind = ConsentLocationMarker.Kind.STAY,
        latitude = latitude,
        longitude = longitude,
        title = "장소",
        snippet = null,
    )

    private fun stay(address: String? = "서울특별시 중구 세종대로 110") =
        SourceItem(
            rawId = "stay-1",
            startAt = START,
            endAt = START.plusSeconds(3_600),
            timeZoneId = ZONE,
            payload = StayPayload(latitude = 37.5665, longitude = 126.9780, address = address),
            sourceName = SourceName.LOCATION_PROVIDER,
            sourceKey = "STAY:stay-1",
            collectedAt = START,
        )

    private fun movement() =
        SourceItem(
            rawId = "move-1",
            startAt = START,
            endAt = START.plusSeconds(1_500),
            timeZoneId = ZONE,
            payload =
                MovementPayload(
                    start = GeoPoint(37.5701, 126.9820, "서울특별시 종로구 종로 1"),
                    end = GeoPoint(37.5512, 126.9882, "서울특별시 중구 남대문로 81"),
                    distanceMeters = 2_400.0,
                    transports = MovementPayload.Transport.WALKING,
                ),
            sourceName = SourceName.LOCATION_PROVIDER,
            sourceKey = "MOVEMENT:move-1",
            collectedAt = START,
        )

    private fun notification() =
        SourceItem(
            rawId = "noti-1",
            startAt = START,
            endAt = null,
            timeZoneId = ZONE,
            payload =
                NotificationPayload(
                    appName = "앱",
                    packageName = "com.example",
                    title = "제목",
                    text = "본문",
                    collectReason = NotificationPayload.CollectReason.KEYWORD,
                ),
            sourceName = SourceName.NOTIFICATION_LISTENER,
            sourceKey = "noti-1",
            collectedAt = START,
        )

    private companion object {
        private val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        private val START: Instant = Instant.parse("2026-08-11T00:10:00Z")
    }
}
