package com.soma369.laimory.core.domain.model.timeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TimelineEventMemoPolicyTest {
    @Test
    fun `한도 안의 메모는 한도까지 받는다`() {
        assertEquals(TimelineEventMemoPolicy.MAX_LENGTH, TimelineEventMemoPolicy.allowedLength(0))
        assertEquals(TimelineEventMemoPolicy.MAX_LENGTH, TimelineEventMemoPolicy.allowedLength(TimelineEventMemoPolicy.MAX_LENGTH))
    }

    @Test
    fun `한도를 넘겨 저장된 메모는 지금 길이까지만 받는다`() {
        // 한도를 줄이기 전에 쓴 글. 한도로 자르면 저장 때 뒷부분이 사라지고, 더 늘리는 입력은 막아야 한다.
        assertEquals(800, TimelineEventMemoPolicy.allowedLength(800))
    }

    @Test
    fun `한도 안의 입력은 그대로 둔다`() {
        assertEquals("가나다라", TimelineEventMemoPolicy.limitInput(previous = "가나다", next = "가나다라"))
    }

    @Test
    fun `한도에 찬 메모의 맨 앞에 넣어도 기존 끝부분은 남는다`() {
        // 새 글 전체를 끝에서 자르면 `끝부분` 이 `끝부` 가 된다(저장 때 그대로 사라진다).
        val previous = "가".repeat(TimelineEventMemoPolicy.MAX_LENGTH - 3) + "끝부분"

        val limited = TimelineEventMemoPolicy.limitInput(previous = previous, next = "추$previous")

        assertEquals(previous, limited)
    }

    @Test
    fun `한도를 넘겨 저장된 메모의 중간에 붙여넣어도 기존 글은 그대로다`() {
        val head = "가".repeat(400)
        val tail = "나".repeat(300) + "끝"
        val previous = head + tail

        val limited = TimelineEventMemoPolicy.limitInput(previous = previous, next = head + "붙여넣은 글" + tail)

        assertEquals(previous, limited)
    }

    @Test
    fun `한도에 여유가 있으면 새 구간을 넘친 만큼만 줄인다`() {
        val previous = "가".repeat(TimelineEventMemoPolicy.MAX_LENGTH - 2) + "끝"
        val pasted = "붙여넣기"

        val limited = TimelineEventMemoPolicy.limitInput(previous = previous, next = "앞" + pasted + previous)

        // 이전 글이 499자라 여유는 1자. 앞에 붙인 구간에서 1자만 받고 기존 글은 모두 남는다.
        assertEquals(TimelineEventMemoPolicy.MAX_LENGTH, limited.length)
        assertEquals("앞$previous", limited)
    }

    @Test
    fun `끝에 붙여넣으면 한도까지만 받는다`() {
        val previous = "가".repeat(TimelineEventMemoPolicy.MAX_LENGTH - 1)

        val limited = TimelineEventMemoPolicy.limitInput(previous = previous, next = previous + "나다라")

        assertEquals(previous + "나", limited)
    }

    @Test
    fun `선택한 글을 더 긴 글로 바꾸면 한도까지 바뀐다`() {
        val previous = "가".repeat(TimelineEventMemoPolicy.MAX_LENGTH - 1) + "끝"
        val replaced = "가".repeat(TimelineEventMemoPolicy.MAX_LENGTH - 1)

        // `끝` 을 `나다라` 로 바꾸는 입력 — 한도를 넘는 2자는 새 구간에서 줄인다.
        val limited = TimelineEventMemoPolicy.limitInput(previous = previous, next = replaced + "나다라")

        assertEquals(replaced + "나", limited)
    }

    @Test
    fun `뒤 절반만 같은 다른 이모지로 바꿔도 반쪽 이모지가 남지 않는다`() {
        // 🔔(D83D DD14) 와 🤔(D83E DD14) 는 하위 서로게이트가 같다. 그 반쪽을 공통 접미부로 두면 깨진다.
        val head = "가".repeat(TimelineEventMemoPolicy.MAX_LENGTH - 2)
        val previous = "$head🔔"

        val limited = TimelineEventMemoPolicy.limitInput(previous = previous, next = "${head}x🤔")

        assertEquals("${head}x", limited)
        assertNoLoneSurrogate(limited)
    }

    @Test
    fun `앞 절반만 같은 다른 이모지로 바꿔도 반쪽 이모지가 남지 않는다`() {
        // 🔔(D83D DD14) 와 🔕(D83D DD15) 는 상위 서로게이트가 같다.
        val head = "가".repeat(TimelineEventMemoPolicy.MAX_LENGTH - 2)
        val previous = "$head🔔"

        val limited = TimelineEventMemoPolicy.limitInput(previous = previous, next = "$head🔕z")

        assertEquals("$head🔕", limited)
        assertNoLoneSurrogate(limited)
    }

    @Test
    fun `줄이다 이모지가 반쪽만 남지 않는다`() {
        val previous = "가".repeat(TimelineEventMemoPolicy.MAX_LENGTH - 1)

        val limited = TimelineEventMemoPolicy.limitInput(previous = previous, next = previous + "😀")

        assertEquals(previous, limited)
        assertNoLoneSurrogate(limited)
    }

    private fun assertNoLoneSurrogate(text: String) {
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c.isHighSurrogate() -> {
                    assertTrue("짝 잃은 상위 서로게이트(index=$i)", i + 1 < text.length && text[i + 1].isLowSurrogate())
                    i += 2
                }
                c.isLowSurrogate() -> fail("짝 잃은 하위 서로게이트(index=$i)")
                else -> i++
            }
        }
    }
}
