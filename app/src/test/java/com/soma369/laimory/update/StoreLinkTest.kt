package com.soma369.laimory.update

import com.soma369.laimory.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreLinkTest {
    @Test
    fun `스토어 주소는 suffix 없는 패키지를 가리킨다`() {
        // 이 테스트는 debug 변종으로 돈다. applicationId 는 `.debug` 지만 스토어에는 그런 앱이 없다.
        assertEquals("com.soma369.laimory.debug", BuildConfig.APPLICATION_ID)
        assertEquals("market://details?id=com.soma369.laimory", StoreLink.marketUri())
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.soma369.laimory",
            StoreLink.webUri(),
        )
    }
}
