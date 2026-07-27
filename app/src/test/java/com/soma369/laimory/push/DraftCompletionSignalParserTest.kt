package com.soma369.laimory.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DraftCompletionSignalParserTest {
    @Test
    fun `SUCCESS와 FAILED 완료 신호를 파싱한다`() {
        assertEquals(
            DraftCompletionSignal("task-1", DraftCompletionStatus.SUCCESS),
            DraftCompletionSignalParser.parse(mapOf("taskId" to "task-1", "status" to "SUCCESS")),
        )
        assertEquals(
            DraftCompletionSignal("task-2", DraftCompletionStatus.FAILED),
            DraftCompletionSignalParser.parse(mapOf("taskId" to "task-2", "status" to "FAILED")),
        )
    }

    @Test
    fun `taskId가 없거나 공백이면 거부한다`() {
        assertNull(DraftCompletionSignalParser.parse(mapOf("status" to "SUCCESS")))
        assertNull(DraftCompletionSignalParser.parse(mapOf("taskId" to "", "status" to "SUCCESS")))
    }

    @Test
    fun `terminal이 아닌 status와 알 수 없는 status는 거부한다`() {
        assertNull(DraftCompletionSignalParser.parse(mapOf("taskId" to "task-1", "status" to "PROCESSING")))
        assertNull(DraftCompletionSignalParser.parse(mapOf("taskId" to "task-1", "status" to "success")))
    }
}
