package com.soma369.laimory.feature.timeline.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimelineMemoDisplayTest {
    @Test
    fun `메모가 있으면 질문보다 메모를 보여준다`() {
        val display = timelineMemoDisplay(memo = "친구를 만났다", question = "누구를 만났나요?", isEditable = true)

        assertEquals(TimelineMemoDisplay.Memo("친구를 만났다"), display)
    }

    @Test
    fun `메모가 비고 질문이 있으면 질문을 안내 문구 자리에 보여준다`() {
        val display = timelineMemoDisplay(memo = null, question = "누구를 만났나요?", isEditable = true)

        assertEquals(TimelineMemoDisplay.Question("누구를 만났나요?"), display)
    }

    @Test
    fun `읽기 모드에서도 메모와 질문은 보여준다`() {
        assertEquals(
            TimelineMemoDisplay.Memo("친구를 만났다"),
            timelineMemoDisplay(memo = "친구를 만났다", question = null, isEditable = false),
        )
        assertEquals(
            TimelineMemoDisplay.Question("오늘 어땠나요?"),
            timelineMemoDisplay(memo = null, question = "오늘 어땠나요?", isEditable = false),
        )
    }

    @Test
    fun `메모와 질문이 모두 없으면 편집 모드에서만 기본 문구를 보여준다`() {
        assertEquals(
            TimelineMemoDisplay.Prompt(DEFAULT_MEMO_PROMPT),
            timelineMemoDisplay(memo = null, question = null, isEditable = true),
        )
        // 읽을 내용이 없는 자리에 누를 수 없는 입력칸을 남기지 않는다.
        assertNull(timelineMemoDisplay(memo = null, question = null, isEditable = false))
    }

    @Test
    fun `공백만 있는 값은 없는 것으로 본다`() {
        // 서버는 공백 question 을 null 로 저장하지만 경계에서 한 번 더 막는다.
        assertEquals(
            TimelineMemoDisplay.Question("무엇을 했나요?"),
            timelineMemoDisplay(memo = "   ", question = "무엇을 했나요?", isEditable = true),
        )
        assertEquals(
            TimelineMemoDisplay.Prompt(DEFAULT_MEMO_PROMPT),
            timelineMemoDisplay(memo = "\n", question = " ", isEditable = true),
        )
        assertNull(timelineMemoDisplay(memo = " ", question = "\t", isEditable = false))
    }
}
