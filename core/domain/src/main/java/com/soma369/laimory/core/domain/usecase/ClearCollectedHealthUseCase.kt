package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.repository.SourceItemRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 스테이징된 건강 데이터를 모두 비운다(재수집 확인용). */
@Singleton
class ClearCollectedHealthUseCase
    @Inject
    constructor(
        private val repository: SourceItemRepository,
    ) {
        suspend operator fun invoke() = repository.clear(ItemType.HEALTH)
    }
