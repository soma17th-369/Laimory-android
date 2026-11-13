package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.repository.SourceItemDeletionRepository
import javax.inject.Inject

/** 방금 지운 수집 아이템을 되돌린다(실행 취소). */
class RestoreSourceItemsUseCase
    @Inject
    constructor(
        private val repository: SourceItemDeletionRepository,
    ) {
        suspend operator fun invoke(items: List<SourceItem>) = repository.restore(items)
    }
