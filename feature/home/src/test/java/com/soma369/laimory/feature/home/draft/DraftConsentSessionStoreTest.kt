package com.soma369.laimory.feature.home.draft

import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelectionPolicy
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import java.time.LocalDate
import java.time.ZoneId

class DraftConsentSessionStoreTest {
    private val zone: ZoneId = ZoneId.systemDefault()
    private val date: LocalDate = LocalDate.now(zone)
    private val store = DraftConsentSessionStore()

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
