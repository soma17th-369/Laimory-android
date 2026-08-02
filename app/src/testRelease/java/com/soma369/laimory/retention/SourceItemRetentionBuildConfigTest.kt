package com.soma369.laimory.retention

import org.junit.Assert.assertEquals
import org.junit.Test

class SourceItemRetentionBuildConfigTest {
    @Test
    fun `release 빌드는 오늘 포함 30일 보존 설정을 주입한다`() {
        assertEquals(30, SourceItemRetentionRuntimeModule.provideSourceItemRetentionConfig().retentionDays)
    }
}
