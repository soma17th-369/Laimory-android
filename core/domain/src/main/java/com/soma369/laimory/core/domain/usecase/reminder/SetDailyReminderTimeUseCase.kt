package com.soma369.laimory.core.domain.usecase.reminder

import com.soma369.laimory.core.domain.repository.DailyReminderRepository
import java.time.LocalTime
import javax.inject.Inject

class SetDailyReminderTimeUseCase
    @Inject
    constructor(
        private val repository: DailyReminderRepository,
    ) {
        suspend operator fun invoke(time: LocalTime) = repository.setTime(time)
    }
