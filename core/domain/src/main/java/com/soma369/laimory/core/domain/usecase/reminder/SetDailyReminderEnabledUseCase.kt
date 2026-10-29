package com.soma369.laimory.core.domain.usecase.reminder

import com.soma369.laimory.core.domain.repository.DailyReminderRepository
import javax.inject.Inject

class SetDailyReminderEnabledUseCase
    @Inject
    constructor(
        private val repository: DailyReminderRepository,
    ) {
        suspend operator fun invoke(isEnabled: Boolean) = repository.setEnabled(isEnabled)
    }
