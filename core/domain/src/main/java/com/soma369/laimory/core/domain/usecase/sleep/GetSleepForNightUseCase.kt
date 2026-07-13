package com.soma369.laimory.core.domain.usecase.sleep

import com.soma369.laimory.core.domain.model.sleep.SleepNightRecord
import com.soma369.laimory.core.domain.repository.SleepRecordRepository
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 그 밤 창에 이미 있는 수면 기록을 조회한다(없으면 null, #145).
 *
 * 결과의 [SleepNightRecord.start]/[SleepNightRecord.end] 로 입력 화면 시간을 프리필하고,
 * [SleepNightRecord.isOurs] 로 저장 정책(외부면 막기 / 우리 것이면 덮어쓰기)을 판단한다.
 */
@Singleton
class GetSleepForNightUseCase
    @Inject
    constructor(
        private val repository: SleepRecordRepository,
    ) {
        suspend operator fun invoke(
            start: Instant,
            end: Instant,
        ): SleepNightRecord? = repository.sleepForNight(start, end)
    }
