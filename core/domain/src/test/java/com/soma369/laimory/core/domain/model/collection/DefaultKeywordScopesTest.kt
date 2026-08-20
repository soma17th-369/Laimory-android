package com.soma369.laimory.core.domain.model.collection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultKeywordScopesTest {
    @Test
    fun `기본 키워드 사전은 scope 의 합집합이다`() {
        val expected =
            setOf(
                "결제", "승인", "환불",
                "주문", "배송", "배달", "픽업",
                "예약", "예매",
                "출발", "도착", "탑승",
                "취소",
            )

        // 어느 scope 에도 속하지 않은 키워드는 게이트 도입과 동시에 죽는다 — 침묵으로 일어나므로 고정한다.
        assertEquals(expected, NotificationFilter.DEFAULT_KEYWORDS)
    }

    @Test
    fun `모든 scope 는 키워드와 앱을 하나 이상 갖는다`() {
        DEFAULT_KEYWORD_SCOPES.forEachIndexed { index, scope ->
            assertTrue("scope $index 키워드", scope.keywords.any(String::isNotBlank))
            assertTrue("scope $index 앱", scope.apps.isNotEmpty())
        }
    }

    @Test
    fun `Prefix 규칙은 다른 scope 의 Exact 를 삼키지 않는다`() {
        val prefixes = DEFAULT_KEYWORD_SCOPES.flatMap { it.apps }.filterIsInstance<AppMatch.Prefix>()
        val exactIds =
            DEFAULT_KEYWORD_SCOPES
                .flatMap { scope -> scope.apps.filterIsInstance<AppMatch.Exact>().map { it.id to scope } }

        prefixes.forEach { prefix ->
            exactIds.forEach { (id, scope) ->
                val sameScope = scope.apps.contains(prefix)
                assertFalse(
                    "${prefix.id} 가 다른 도메인의 $id 를 삼킨다",
                    !sameScope && prefix.covers(id),
                )
            }
        }
    }

    @Test
    fun `Exact 는 정확히 일치할 때만 통과시킨다`() {
        val exact = AppMatch.Exact("com.coupang.mobile")

        assertTrue(exact.covers("com.coupang.mobile"))
        assertFalse(exact.covers("com.coupang.mobile.eats"))
        assertFalse(exact.covers("com.coupang"))
    }

    @Test
    fun `Prefix 는 구분자 경계를 요구한다`() {
        val prefix = AppMatch.Prefix("com.example")

        assertTrue(prefix.covers("com.example"))
        assertTrue(prefix.covers("com.example.pay"))
        // 경계 없이 이어지는 다른 앱은 삼키지 않는다.
        assertFalse(prefix.covers("com.exampleother"))
    }

    @Test
    fun `쿠팡과 쿠팡이츠는 서로 다른 도메인으로 갈린다`() {
        // Exact 를 기본으로 둔 이유다 — 접두였다면 com.coupang.mobile 이 이츠까지 삼킨다.
        val commerce = DEFAULT_KEYWORD_SCOPES.first { it.keywords.contains("배송") }
        val delivery = DEFAULT_KEYWORD_SCOPES.first { it.keywords.contains("배달") }

        assertTrue(commerce.apps.any { it.covers("com.coupang.mobile") })
        assertFalse(commerce.apps.any { it.covers("com.coupang.mobile.eats") })
        assertTrue(delivery.apps.any { it.covers("com.coupang.mobile.eats") })
    }
}
