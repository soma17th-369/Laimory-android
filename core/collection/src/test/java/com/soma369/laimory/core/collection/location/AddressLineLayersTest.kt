package com.soma369.laimory.core.collection.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AddressLineLayersTest {
    @Test
    fun `시와 동을 한 줄에서 골라낸다`() {
        val layers = addressLineLayers("대한민국 오산시 부산동 305")

        assertEquals("오산시", layers.city)
        assertEquals("부산동", layers.district)
    }

    @Test
    fun `꼬리말이 겹치면 더 좁은 뒤엣것을 고른다`() {
        // `서울특별시` 도 `시` 로 끝나지만 우리가 찾는 것은 `강남구` 다.
        val layers = addressLineLayers("대한민국 서울특별시 강남구 역삼동 823")

        assertEquals("강남구", layers.city)
        assertEquals("역삼동", layers.district)
    }

    @Test
    fun `도로명 주소에는 동 층위가 없다`() {
        val layers = addressLineLayers("대한민국 서울특별시 종로구 종로 1")

        assertEquals("종로구", layers.city)
        assertNull(layers.district)
    }

    @Test
    fun `찾을 것이 없으면 비운다`() {
        val layers = addressLineLayers("Some Street 12")

        assertNull(layers.city)
        assertNull(layers.district)
    }
}
