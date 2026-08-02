package com.soma369.laimory.retention

import org.junit.Assert.assertEquals
import org.junit.Test

class SourceItemRetentionBuildConfigTest {
    @Test
    fun `debug 빌드는 오늘 포함 365일 보존 설정을 주입한다`() {
        assertEquals(365, SourceItemRetentionRuntimeModule.provideSourceItemRetentionConfig().retentionDays)
    }
}
