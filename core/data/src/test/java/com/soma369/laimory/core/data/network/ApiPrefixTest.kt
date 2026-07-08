package com.soma369.laimory.core.data.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiPrefixTest {
    @Test
    fun `public baseUrl 은 루트-api-버전 순으로 조합되고 슬래시로 끝난다`() {
        assertEquals("https://example.test/api/v9/", ApiPrefix.publicBaseUrl("https://example.test/", "v9"))
    }

    @Test
    fun `루트 끝 슬래시 유무와 무관하게 같은 결과다`() {
        assertEquals("https://example.test/api/v9/", ApiPrefix.publicBaseUrl("https://example.test", "v9"))
    }
}
