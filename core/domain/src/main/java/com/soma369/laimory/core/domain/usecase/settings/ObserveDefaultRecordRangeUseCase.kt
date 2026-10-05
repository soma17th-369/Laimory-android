package com.soma369.laimory.core.domain.usecase.settings

import com.soma369.laimory.core.domain.model.settings.DefaultRecordRange
import com.soma369.laimory.core.domain.repository.DefaultRecordRangeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** 홈이 첫 범위를 정하고, 날짜 피커가 `기본값으로 지정` 의 처음 상태를 정하는 근거다. */
@Singleton
class ObserveDefaultRecordRangeUseCase
    @Inject
    constructor(
        private val repository: DefaultRecordRangeRepository,
    ) {
        operator fun invoke(): Flow<DefaultRecordRange> = repository.range
    }
