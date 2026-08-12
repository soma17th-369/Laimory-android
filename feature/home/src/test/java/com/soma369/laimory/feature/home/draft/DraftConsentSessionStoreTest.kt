package com.soma369.laimory.feature.home.draft

import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionPolicy
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DraftConsentSessionStoreTest {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val date: LocalDate = LocalDate.now(zone)
    private val store = DraftConsentSessionStore()

    @Test
    fun `clearAll 은 준비 상태와 일회성 결과를 모두 초기화한다`() {
        prepare()
        store.markSubmitted()
        store.markPhotoReselectionNeeded()

        store.clearAll()

        assertNull(store.preparation.value)
        assertFalse(store.consumeSubmittedResult())
        assertFalse(store.consumePhotoReselectionNeeded())
    }

    @Test
    fun `일회성 신호는 한 번만 소비된다`() {
        store.markSubmitted()
        store.markPhotoReselectionNeeded()

        assertTrue(store.consumeSubmittedResult())
        assertFalse(store.consumeSubmittedResult())
        assertTrue(store.consumePhotoReselectionNeeded())
        assertFalse(store.consumePhotoReselectionNeeded())
    }

    private fun prepare() {
        val window = RecordDateWindow.ofDate(date, zone)
        store.prepare(
            recordDate = date,
            zone = zone,
            window = window,
            selection = DraftSourceItemSelectionPolicy().select(window, emptyList()).getOrThrow(),
            discardActiveTask = false,
        )
    }
}
