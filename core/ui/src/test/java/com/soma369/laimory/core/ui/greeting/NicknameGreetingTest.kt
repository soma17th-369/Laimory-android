package com.soma369.laimory.core.ui.greeting

import org.junit.Assert.assertEquals
import org.junit.Test

class NicknameGreetingTest {
    @Test
    fun `닉네임이 있으면 이름만 강조한 세 조각으로 나눈다`() {
        val segments = nicknameGreetingSegments("김소마")

        assertEquals(
            listOf(
                GreetingSegment("안녕하세요, ", GreetingEmphasis.NORMAL),
                GreetingSegment("김소마", GreetingEmphasis.NICKNAME),
                // Figma 는 `님`을 강조하지 않는다.
                GreetingSegment("님", GreetingEmphasis.NORMAL),
            ),
            segments,
        )
    }

    @Test
    fun `합치면 한 문장이 된다`() {
        // 접근성 서비스가 한 번에 읽도록 화면은 이 조각들을 한 AnnotatedString 으로 합친다.
        assertEquals("안녕하세요, 김소마님", nicknameGreetingSegments("김소마").joinToString("") { it.text })
    }

    @Test
    fun `닉네임이 없으면 님까지 빠진다`() {
        val expected = listOf(GreetingSegment("안녕하세요", GreetingEmphasis.NORMAL))

        assertEquals(expected, nicknameGreetingSegments(null))
        assertEquals(expected, nicknameGreetingSegments(""))
        assertEquals(expected, nicknameGreetingSegments("   "))
    }

    @Test
    fun `앞뒤 공백은 다듬어 문장이 벌어지지 않게 한다`() {
        assertEquals("안녕하세요, 김소마님", nicknameGreetingSegments(" 김소마 ").joinToString("") { it.text })
    }
}
