package com.soma369.laimory.update

import org.junit.Assert.assertEquals
import org.junit.Test

class StoreLinkTest {
    @Test
    fun `스토어 주소는 suffix 없는 패키지를 가리킨다`() {
        // 이 파일은 세 변종에서 모두 돈다. 설치 패키지명은 변종마다 다르므로 여기서 단정하지 않고,
        // **스토어가 가리키는 곳**만 본다 — debug 는 `.debug`, qa 는 `.qa` 가 붙는데 스토어에는
        // 그런 앱이 없다.
        assertEquals("market://details?id=com.soma369.laimory", StoreLink.marketUri())
        assertEquals(
            "https://play.google.com/store/apps/details?id=com.soma369.laimory",
            StoreLink.webUri(),
        )
    }
}
