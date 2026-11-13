package com.soma369.laimory.core.data.di

import com.soma369.laimory.core.data.BuildConfig
import com.soma369.laimory.core.data.network.ApiOriginOverride
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Optional

class ApiOriginModuleTest {
    @Test
    fun `덮어쓸 서버가 없으면 빌드 값을 쓴다`() {
        assertEquals(BuildConfig.BASE_URL, ApiOriginModule.provideApiBaseUrl(Optional.empty()))
    }

    @Test
    fun `덮어쓸 서버가 있으면 그 origin 을 쓴다`() {
        val override =
            object : ApiOriginOverride {
                override val baseUrl = "https://api.laimory.app/"
            }

        assertEquals("https://api.laimory.app/", ApiOriginModule.provideApiBaseUrl(Optional.of(override)))
    }
}
