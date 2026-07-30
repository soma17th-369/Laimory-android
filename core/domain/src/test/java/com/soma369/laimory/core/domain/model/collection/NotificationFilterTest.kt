package com.soma369.laimory.core.domain.model.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationFilterTest {
    @Test
    fun `클릭한 알림은 필터가 비어 있어도 수집한다`() {
        val reason =
            NotificationFilter().collectReasonFor(
                packageName = "com.example",
                title = null,
                text = null,
                clicked = true,
            )

        assertEquals(NotificationPayload.CollectReason.CLICK, reason)
    }

    @Test
    fun `클릭 수집을 끄면 앱과 키워드가 일치해도 클릭 이벤트에서는 수집하지 않는다`() {
        val reason =
            NotificationFilter(
                collectOnClick = false,
                keywords = setOf("회의"),
                allowedPackages = setOf("com.example.allowed"),
            ).collectReasonFor(
                packageName = "com.example.allowed",
                title = "회의 시작",
                text = null,
                clicked = true,
            )

        assertNull(reason)
    }

    @Test
    fun `클릭 수집을 꺼도 게시 알림의 앱과 키워드 필터는 동작한다`() {
        val reason =
            NotificationFilter(
                collectOnClick = false,
                allowedPackages = setOf("com.example.allowed"),
            ).collectReasonFor(
                packageName = "com.example.allowed",
                title = "일반 알림",
                text = null,
                clicked = false,
            )

        assertEquals(NotificationPayload.CollectReason.APP, reason)
    }

    @Test
    fun `선택 앱의 게시 알림을 수집한다`() {
        val reason =
            NotificationFilter(
                allowedPackages = setOf("com.example.allowed"),
            ).collectReasonFor(
                packageName = "com.example.allowed",
                title = "제목",
                text = "본문",
                clicked = false,
            )

        assertEquals(NotificationPayload.CollectReason.APP, reason)
    }

    @Test
    fun `제목이나 본문의 키워드는 대소문자 구분 없이 수집한다`() {
        val filter = NotificationFilter(keywords = setOf("Laimory"))

        val titleReason =
            filter.collectReasonFor(
                packageName = "com.example",
                title = "LAIMORY 알림",
                text = null,
                clicked = false,
            )
        val textReason =
            filter.collectReasonFor(
                packageName = "com.example",
                title = null,
                text = "오늘 laimory 기록을 확인하세요",
                clicked = false,
            )

        assertEquals(NotificationPayload.CollectReason.KEYWORD, titleReason)
        assertEquals(NotificationPayload.CollectReason.KEYWORD, textReason)
    }

    @Test
    fun `키워드와 앱이 함께 일치하면 키워드를 대표 사유로 사용한다`() {
        val reason =
            NotificationFilter(
                keywords = setOf("회의"),
                allowedPackages = setOf("com.example.allowed"),
            ).collectReasonFor(
                packageName = "com.example.allowed",
                title = "회의 시작",
                text = null,
                clicked = false,
            )

        assertEquals(NotificationPayload.CollectReason.KEYWORD, reason)
    }

    @Test
    fun `빈 키워드와 불일치 앱의 게시 알림은 수집하지 않는다`() {
        val reason =
            NotificationFilter(
                keywords = setOf("", "   "),
                allowedPackages = setOf("com.example.allowed"),
            ).collectReasonFor(
                packageName = "com.example.other",
                title = "일반 알림",
                text = "필터와 일치하지 않음",
                clicked = false,
            )

        assertNull(reason)
    }
}
