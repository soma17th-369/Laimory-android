package com.soma369.laimory.core.domain.usecase.settings

import com.soma369.laimory.core.domain.model.settings.DefaultRecordRange
import com.soma369.laimory.core.domain.repository.DefaultRecordRangeRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 고른 범위를 기본값으로 저장한다.
 *
 * 실패를 값으로 돌려준다 — 저장에 실패했는데 알리지 않으면, 다음 실행에서 되돌아온 범위를 보고 앱이 제 설정을
 * 잊었다고 여기게 된다.
 */
@Singleton
class SetDefaultRecordRangeUseCase
    @Inject
    constructor(
        private val repository: DefaultRecordRangeRepository,
    ) {
        suspend operator fun invoke(range: DefaultRecordRange): Result<Unit> = runCatching { repository.setRange(range) }
    }
