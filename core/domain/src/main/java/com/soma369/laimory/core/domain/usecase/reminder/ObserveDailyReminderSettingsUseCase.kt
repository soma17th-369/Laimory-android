package com.soma369.laimory.core.domain.usecase.reminder

import com.soma369.laimory.core.domain.model.reminder.DailyReminderSettings
import com.soma369.laimory.core.domain.repository.DailyReminderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDailyReminderSettingsUseCase
    @Inject
    constructor(
        private val repository: DailyReminderRepository,
    ) {
        operator fun invoke(): Flow<DailyReminderSettings> = repository.settings
    }
