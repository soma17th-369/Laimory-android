package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.repository.SourceItemRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 스테이징된 캘린더 일정을 모두 비운다(재수집 확인용). */
@Singleton
class ClearCollectedCalendarUseCase
    @Inject
    constructor(
        private val repository: SourceItemRepository,
    ) {
        suspend operator fun invoke() = repository.clear(ItemType.CALENDAR)
    }
