package com.soma369.laimory

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI
import com.soma369.laimory.core.data.BuildConfig as DataBuildConfig

class AppLinkBuildConfigTest {
    @Test
    fun `debug API와 callback은 같은 HTTPS origin을 사용한다`() {
        val apiOrigin = URI(DataBuildConfig.BASE_URL)

        assertEquals("https", apiOrigin.scheme)
        assertEquals(apiOrigin.host, BuildConfig.AUTH_CALLBACK_HOST)
    }
}
