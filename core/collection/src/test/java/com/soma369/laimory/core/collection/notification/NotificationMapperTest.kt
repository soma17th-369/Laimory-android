package com.soma369.laimory.core.collection.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationMapperTest {
    @Test
    fun `진행률이 남아 있어도 완료 상태는 진행 중으로 보지 않는다`() {
        assertFalse(isProgressOngoing(max = 100, progress = 100, indeterminate = false))
    }

    @Test
    fun `진행 표시를 끈 알림은 진행 중으로 보지 않는다`() {
        // setProgress(0, 0, false) 로 표시를 해제해도 extras 키는 남는다.
        assertFalse(isProgressOngoing(max = 0, progress = 0, indeterminate = false))
    }

    @Test
    fun `진행률이 최대에 못 미치면 진행 중으로 본다`() {
        assertTrue(isProgressOngoing(max = 100, progress = 40, indeterminate = false))
    }

    @Test
    fun `불확정 진행률은 최대값과 무관하게 진행 중으로 본다`() {
        assertTrue(isProgressOngoing(max = 0, progress = 0, indeterminate = true))
    }

    @Test
    fun `커스텀 뷰 템플릿에 표준 본문이 없으면 읽을 수 없는 본문으로 본다`() {
        CUSTOM_CONTENT_TEMPLATES.forEach { template ->
            assertTrue(hasUnreadableBody(template = template, text = null))
            assertTrue(hasUnreadableBody(template = template, text = "  "))
        }
    }

    @Test
    fun `커스텀 뷰 템플릿이어도 표준 본문이 있으면 읽을 수 있는 본문으로 본다`() {
        // 커스텀 뷰를 쓰면서 EXTRA_TEXT 도 채우는 앱은 광고 표기 판정이 성립한다.
        assertFalse(hasUnreadableBody(template = CUSTOM_CONTENT_TEMPLATES.first(), text = "배송 출발"))
    }

    @Test
    fun `표준 템플릿은 본문이 비어 있어도 읽을 수 없는 본문으로 보지 않는다`() {
        assertFalse(hasUnreadableBody(template = null, text = null))
        assertFalse(hasUnreadableBody(template = "android.app.Notification\$BigTextStyle", text = null))
    }
}
