package com.soma369.laimory.core.collection.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationMapperTest {
    @Test
    fun `제목과 본문이 모두 null이면 수집하지 않는다`() {
        assertFalse(hasCollectibleContent(title = null, text = null))
    }

    @Test
    fun `제목과 본문이 모두 blank이면 수집하지 않는다`() {
        assertFalse(hasCollectibleContent(title = "  ", text = "\n"))
    }

    @Test
    fun `제목이나 본문 중 하나가 있으면 수집한다`() {
        assertTrue(hasCollectibleContent(title = "일정 알림", text = null))
        assertTrue(hasCollectibleContent(title = null, text = "회의가 곧 시작됩니다"))
    }
}
