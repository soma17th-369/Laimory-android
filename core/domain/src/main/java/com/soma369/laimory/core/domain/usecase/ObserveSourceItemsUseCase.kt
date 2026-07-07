package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.repository.SourceItemRepository
import kotlinx.coroutines.flow.Flow

/** 저장된 수집 아이템 전체를 이벤트 시각 내림차순으로 관찰한다. */
class ObserveSourceItemsUseCase(
    private val repository: SourceItemRepository,
) {
    operator fun invoke(): Flow<List<SourceItem>> = repository.observeAll()
}
