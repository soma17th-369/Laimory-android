package com.soma369.laimory.core.collection.health

import com.soma369.laimory.core.domain.repository.SleepRecordRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * [SleepRecordRepository] 의 구현 — #143 [SleepHealthRecorder] 를 도메인 계약에 노출한다.
 *
 * 사용자 입력(#145)·감지(#144) 공통의 HC 기록 배관을 재사용한다.
 */
internal class SleepRecordRepositoryImpl
    @Inject
    constructor(
        private val recorder: SleepHealthRecorder,
    ) : SleepRecordRepository {
        override suspend fun hasSleep(
            start: Instant,
            end: Instant,
        ): Boolean = recorder.hasAnySleep(start, end)

        override suspend fun hasExternalSleep(
            start: Instant,
            end: Instant,
        ): Boolean = recorder.hasExternalSleep(start, end)

        override suspend fun recordManualSleep(
            night: LocalDate,
            start: Instant,
            end: Instant,
            zoneOffset: ZoneOffset?,
        ) = recorder.recordManualSleep(night, start, end, zoneOffset)
    }
