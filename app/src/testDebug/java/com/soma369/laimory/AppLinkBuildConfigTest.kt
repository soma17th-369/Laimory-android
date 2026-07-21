package com.soma369.laimory

import org.junit.Assert.assertEquals
import org.junit.Test
import com.soma369.laimory.core.data.BuildConfig as DataBuildConfig

class AppLinkBuildConfigTest {
    @Test
    fun `debug API와 callback은 개발 도메인을 사용한다`() {
        assertEquals("dev.laimory.app", BuildConfig.AUTH_CALLBACK_HOST)
        assertEquals("https://dev.laimory.app/", DataBuildConfig.BASE_URL)
    }
}
