package com.soma369.laimory.core.domain.model.timeline

import org.junit.Assert.assertEquals
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
}
