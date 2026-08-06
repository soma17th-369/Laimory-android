package com.soma369.laimory.feature.collection.screen

import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.model.collection.StayPayload
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationCollectionTabTest {
    @Test
    fun `STAY는 주소와 좌표를 각각 표시할 수 있다`() {
        val payload = StayPayload(latitude = 37.5, longitude = 126.9, address = "서울특별시 마포구")

        assertEquals("서울특별시 마포구", payload.displayAddress())
        assertEquals("37.50000, 126.90000", payload.coord())
    }

    @Test
    fun `MOVEMENT 좌표도 주소와 위경도를 각각 표시할 수 있다`() {
        val point = GeoPoint(latitude = 37.5, longitude = 126.9, address = "서울특별시 마포구")

        assertEquals("서울특별시 마포구", point.displayAddress())
        assertEquals("37.50000, 126.90000", point.coord())
    }
}
