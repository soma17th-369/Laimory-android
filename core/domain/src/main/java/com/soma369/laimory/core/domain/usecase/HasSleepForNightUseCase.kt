package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.repository.SleepRecordRepository
import java.time.Instant

/** 그 밤 창에 이미 수면 기록이 있는지 조회한다(입력 유도/중복 안내용, #145). */
class HasSleepForNightUseCase(
    private val repository: SleepRecordRepository,
) {
    suspend operator fun invoke(
        start: Instant,
        end: Instant,
    ): Boolean = repository.hasSleep(start, end)
}
