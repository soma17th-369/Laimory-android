package com.soma369.laimory.feature.home.draft

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DraftLoadingSessionStoreTest {
    private val store = DraftLoadingSessionStore()

    @Test
    fun `작업 번호를 받기 전의 새 요청은 이전 작업과 다른 시도다`() {
        // 이전 작업의 완료가 남은 채 새 요청의 로딩 화면이 뜨면, 그 완료로 화면을 옮기면 안 된다.
        store.start(session(taskId = null))

        assertTrue(store.showsOtherAttemptThan("old"))
    }

    @Test
    fun `같은 작업이면 다른 시도가 아니고 다른 작업이면 다른 시도다`() {
        store.start(session(taskId = null))
        store.attachTask("new")

        assertFalse(store.showsOtherAttemptThan("new"))
        assertTrue(store.showsOtherAttemptThan("old"))
    }

    @Test
    fun `스냅샷이 없으면 판단하지 않는다`() {
        // 프로세스 재시작 뒤 알림으로 들어온 로딩 화면은 스냅샷이 없다. 그때도 완료로 넘어가야 한다.
        assertFalse(store.showsOtherAttemptThan("restored"))
    }

    private fun session(taskId: String?) =
        DraftLoadingSession(
            taskId = taskId,
            recordDate = LocalDate.of(2026, 10, 2),
            photoUris = emptyList(),
            photoCount = 0,
            calendarCount = 0,
            stayCount = 0,
        )
}
