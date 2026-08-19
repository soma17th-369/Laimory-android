package com.soma369.laimory.feature.home.greeting

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeGreetingTest {
    @Test
    fun `닉네임이 있으면 이름만 강조한 세 조각으로 나눈다`() {
        val segments = homeGreetingSegments("김소마")

        assertEquals(
            listOf(
                HomeGreetingSegment("안녕하세요, ", HomeGreetingEmphasis.NORMAL),
                HomeGreetingSegment("김소마", HomeGreetingEmphasis.NICKNAME),
                // Figma 는 `님`을 강조하지 않는다.
                HomeGreetingSegment("님", HomeGreetingEmphasis.NORMAL),
            ),
            segments,
        )
    }

    @Test
    fun `합치면 한 문장이 된다`() {
        // 접근성 서비스가 한 번에 읽도록 화면은 이 조각들을 한 AnnotatedString 으로 합친다.
        assertEquals("안녕하세요, 김소마님", homeGreetingSegments("김소마").joinToString("") { it.text })
    }

    @Test
    fun `닉네임이 없으면 님까지 빠진다`() {
        val expected = listOf(HomeGreetingSegment("안녕하세요", HomeGreetingEmphasis.NORMAL))

        assertEquals(expected, homeGreetingSegments(null))
        assertEquals(expected, homeGreetingSegments(""))
        assertEquals(expected, homeGreetingSegments("   "))
    }

    @Test
    fun `앞뒤 공백은 다듬어 문장이 벌어지지 않게 한다`() {
        assertEquals("안녕하세요, 김소마님", homeGreetingSegments(" 김소마 ").joinToString("") { it.text })
    }
}
