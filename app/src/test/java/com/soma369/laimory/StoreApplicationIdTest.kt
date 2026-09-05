package com.soma369.laimory

import org.junit.Assert.assertEquals
import org.junit.Test

class StoreApplicationIdTest {
    @Test
    fun `스토어 주소는 어느 빌드에서든 suffix 없는 applicationId 다`() {
        // debug 는 `.debug`, qa 는 `.qa` 가 붙는다. 스토어를 열 때 자기 applicationId 를 쓰면
        // 스토어에 없는 앱을 찾게 되므로, 세 빌드가 모두 release 의 값을 들고 있어야 한다.
        assertEquals("com.soma369.laimory", BuildConfig.STORE_APPLICATION_ID)
    }
}
