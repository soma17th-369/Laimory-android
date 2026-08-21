package com.soma369.laimory.feature.timeline.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineMemoDisplayTest {
    @Test
    fun `메모가 있으면 질문보다 메모를 보여준다`() {
        val display = timelineMemoDisplay(memo = "친구를 만났다", question = "누구를 만났나요?")

        assertEquals(TimelineMemoDisplay.Memo("친구를 만났다"), display)
    }

    @Test
    fun `메모가 비고 질문이 있으면 질문을 안내 문구 자리에 보여준다`() {
        val display = timelineMemoDisplay(memo = null, question = "누구를 만났나요?")

        assertEquals(TimelineMemoDisplay.Question("누구를 만났나요?"), display)
    }

    @Test
    fun `메모와 질문이 모두 없으면 기본 문구를 보여준다`() {
        // 표시는 모드와 무관하다. 편집 가능 여부는 클릭에서만 가른다 —
        // 모드에 따라 영역을 감추면 카드 높이가 달라져 전환할 때 목록이 밀린다.
        assertEquals(
            TimelineMemoDisplay.Prompt(DEFAULT_MEMO_PROMPT),
            timelineMemoDisplay(memo = null, question = null),
        )
    }

    @Test
    fun `공백만 있는 값은 없는 것으로 본다`() {
        // 서버는 공백 question 을 null 로 저장하지만 경계에서 한 번 더 막는다.
        assertEquals(
            TimelineMemoDisplay.Question("무엇을 했나요?"),
            timelineMemoDisplay(memo = "   ", question = "무엇을 했나요?"),
        )
        assertEquals(
            TimelineMemoDisplay.Prompt(DEFAULT_MEMO_PROMPT),
            timelineMemoDisplay(memo = "\n", question = " "),
        )
    }
}
