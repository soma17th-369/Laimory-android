package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.repository.SourceItemRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 수집 아이템을 로컬 저장소에 저장한다.
 *
 * 동일 (itemType, sourceName, sourceKey) 아이템은 무시된다(멱등).
 *
 * @return 실제로 새로 저장된 아이템 수
 */
@Singleton
class AddSourceItemsUseCase
    @Inject
    constructor(
        private val repository: SourceItemRepository,
    ) {
        suspend operator fun invoke(items: List<SourceItem>): Int = repository.addAll(items)
    }
