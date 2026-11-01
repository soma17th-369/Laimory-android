package com.soma369.laimory.core.domain.usecase.reminder

import com.soma369.laimory.core.domain.model.reminder.DailyReminderRecordDay
import com.soma369.laimory.core.domain.repository.DailyReminderRepository
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * 지금 기록 날짜의 리마인더를 이미 띄웠는지.
 *
 * 띄운 날에는 시간대를 바꿔도 내일부터 적용된다. 설정 화면이 그 사실을 알려 줄 때 쓴다 — 알리지 않으면 바꾼
 * 시각에 알림을 기다리다 "설정했는데 안 온다"고 느낀다.
 */
class IsDailyReminderDeliveredTodayUseCase
    @Inject
    constructor(
        private val repository: DailyReminderRepository,
    ) {
        suspend operator fun invoke(now: LocalDateTime = LocalDateTime.now()): Boolean =
            repository.deliveryMemory().lastDeliveredRecordDate == DailyReminderRecordDay.of(now)
    }
