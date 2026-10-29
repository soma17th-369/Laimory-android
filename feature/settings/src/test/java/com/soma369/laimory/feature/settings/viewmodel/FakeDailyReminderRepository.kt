package com.soma369.laimory.feature.settings.viewmodel

import com.soma369.laimory.core.domain.model.reminder.DailyReminderDeliveryMemory
import com.soma369.laimory.core.domain.model.reminder.DailyReminderSettings
import com.soma369.laimory.core.domain.repository.DailyReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate
import java.time.LocalTime

/** 일일 리마인더 저장소 테스트 대역. 메모리에만 둔다. */
class FakeDailyReminderRepository(
    initialSettings: DailyReminderSettings = DailyReminderSettings.DEFAULT,
    var storedEnabled: Boolean = false,
) : DailyReminderRepository {
    private val state = MutableStateFlow(initialSettings)
    private val handedOff = MutableStateFlow(false)
    private val mirror = MutableStateFlow<Boolean?>(null)
    var savedRecordDates = mutableListOf<LocalDate>()

    override val settings: Flow<DailyReminderSettings> = state
    override val isServerReminderHandedOff: Flow<Boolean> = handedOff
    override val pushEnabledMirror: Flow<Boolean?> = mirror

    val currentSettings: DailyReminderSettings get() = state.value
    val isHandedOff: Boolean get() = handedOff.value
    val mirrorValue: Boolean? get() = mirror.value

    override suspend fun hasStoredEnabled(): Boolean = storedEnabled

    override suspend fun setEnabled(isEnabled: Boolean) {
        storedEnabled = true
        state.value = state.value.copy(isEnabled = isEnabled)
    }

    override suspend fun setTime(time: LocalTime) {
        state.value = state.value.copy(time = time)
    }

    override suspend fun markServerReminderHandedOff() {
        handedOff.value = true
    }

    override suspend fun setPushEnabledMirror(isEnabled: Boolean) {
        mirror.value = isEnabled
    }

    override suspend fun deliveryMemory(): DailyReminderDeliveryMemory =
        DailyReminderDeliveryMemory(savedRecordDates.lastOrNull(), null, null, null)

    override suspend fun markRecordSaved(recordDate: LocalDate) {
        savedRecordDates += recordDate
    }

    override suspend fun setLastGeneralTitleIndex(index: Int) = Unit

    override suspend fun setLastCompletedCopyIndex(index: Int) = Unit

    override suspend fun setLastDeliveredRecordDate(date: LocalDate) = Unit
}
