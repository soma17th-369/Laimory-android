package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.repository.SleepRecordRepository
import java.time.Instant

/** 그 밤 창에 외부 앱이 쓴 수면이 있는지(#145). 저장 시 덮어쓰기 불가·중복 경고 문구 판단에 쓴다. */
class HasExternalSleepForNightUseCase(
    private val repository: SleepRecordRepository,
) {
    suspend operator fun invoke(
        start: Instant,
        end: Instant,
    ): Boolean = repository.hasExternalSleep(start, end)
}
