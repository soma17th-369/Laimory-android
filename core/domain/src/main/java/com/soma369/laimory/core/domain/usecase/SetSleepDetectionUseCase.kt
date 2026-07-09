package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.repository.SleepDetectionRepository

/** 수면 자동 감지를 켜거나 끈다(#142). 켜기 전 활동 인식·HC 쓰기 권한 확보는 화면이 선행한다. */
class SetSleepDetectionUseCase(
    private val repository: SleepDetectionRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setEnabled(enabled)
}
