package com.soma369.laimory

import org.junit.Assert.assertEquals
import org.junit.Test
import com.soma369.laimory.core.data.BuildConfig as DataBuildConfig

class AppLinkBuildConfigTest {
    @Test
    fun `release API와 callback은 운영 도메인을 사용한다`() {
        assertEquals("laimory.app", BuildConfig.AUTH_CALLBACK_HOST)
        assertEquals("https://laimory.app/", DataBuildConfig.BASE_URL)
    }
}
