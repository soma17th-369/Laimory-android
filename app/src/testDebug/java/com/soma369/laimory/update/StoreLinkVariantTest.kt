package com.soma369.laimory.update

import com.soma369.laimory.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class StoreLinkVariantTest {
    @Test
    fun `debug 는 설치 패키지와 스토어 주소가 다르다`() {
        // 자기 applicationId 로 스토어를 열면 없는 앱을 찾게 된다. 그 둘이 실제로 갈리는 변종에서
        // 확인한다 — release 는 두 값이 같아 이 검증이 성립하지 않는다.
        assertEquals("com.soma369.laimory.debug", BuildConfig.APPLICATION_ID)
        assertNotEquals(BuildConfig.APPLICATION_ID, BuildConfig.STORE_APPLICATION_ID)
        assertEquals("com.soma369.laimory", BuildConfig.STORE_APPLICATION_ID)
    }
}
