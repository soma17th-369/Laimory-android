package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.source.CalendarSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** 시스템 캘린더 내용이 바뀔 때마다 신호를 낸다. 받은 쪽이 [GetCalendarEventsInWindowUseCase] 로 다시 읽는다. */
@Singleton
class ObserveCalendarChangesUseCase
    @Inject
    constructor(
        private val calendarSource: CalendarSource,
    ) {
        operator fun invoke(): Flow<Unit> = calendarSource.changes()
    }
