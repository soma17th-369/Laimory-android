package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.repository.SleepRecordRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

/** 사용자가 입력한 수면(취침~기상)을 Health Connect 에 기록한다(불확실한 밤 폴백, #145). */
@Singleton
class RecordManualSleepUseCase
    @Inject
    constructor(
        private val repository: SleepRecordRepository,
    ) {
        suspend operator fun invoke(
            night: LocalDate,
            start: Instant,
            end: Instant,
            zoneOffset: ZoneOffset?,
        ) = repository.recordManualSleep(night, start, end, zoneOffset)
    }
