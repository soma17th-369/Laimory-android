package com.soma369.laimory.feature.timeline.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimelineMemoDisplayTest {
    @Test
    fun `메모가 있으면 메모를 보여준다`() {
        val display = timelineMemoDisplay(memo = "친구를 만났다", isEditable = true)

        assertEquals(TimelineMemoDisplay.Memo("친구를 만났다"), display)
    }

    @Test
    fun `메모가 비면 안내 문구를 띄운다`() {
        // 질문이 있든 없든 같은 문구다. 질문 자체는 말풍선이 보여 준다.
        assertEquals(
            TimelineMemoDisplay.Prompt(MEMO_PROMPT),
            timelineMemoDisplay(memo = null, isEditable = true),
        )
    }

    @Test
    fun `읽기 모드는 사용자가 남긴 메모만 보여준다`() {
        assertEquals(
            TimelineMemoDisplay.Memo("친구를 만났다"),
            timelineMemoDisplay(memo = "친구를 만났다", isEditable = false),
        )
        // 읽을 내용이 없는 자리에 누를 수 없는 입력칸을 남기지 않는다.
        assertNull(timelineMemoDisplay(memo = null, isEditable = false))
    }

    @Test
    fun `질문 말풍선은 편집 모드에서만 뜬다`() {
        assertEquals("오늘 어땠나요?", timelineMemoQuestion(question = "오늘 어땠나요?", isEditable = true))
        // 메모를 이미 남겼어도 편집 모드에서는 계속 띄운다 — 무엇에 답한 글인지가 문맥이다.
        assertEquals("오늘 어땠나요?", timelineMemoQuestion(question = "오늘 어땠나요?", isEditable = true))
        // 저장을 마친 화면에서 답할 수 없는 질문은 읽을거리가 아니다.
        assertNull(timelineMemoQuestion(question = "오늘 어땠나요?", isEditable = false))
    }

    @Test
    fun `공백만 있는 값은 없는 것으로 본다`() {
        assertEquals(
            TimelineMemoDisplay.Prompt(MEMO_PROMPT),
            timelineMemoDisplay(memo = "   ", isEditable = true),
        )
        assertEquals(
            TimelineMemoDisplay.Prompt(MEMO_PROMPT),
            timelineMemoDisplay(memo = "\n", isEditable = true),
        )
        // 서버는 공백 question 을 null 로 저장하지만 경계에서 한 번 더 막는다.
        assertNull(timelineMemoQuestion(question = " ", isEditable = true))
        // 읽기 모드는 공백 메모를 없는 것으로 보고 영역째 감춘다.
        assertNull(timelineMemoDisplay(memo = " ", isEditable = false))
    }
}
