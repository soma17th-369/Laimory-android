package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.repository.SourceItemDeletionRepository
import javax.inject.Inject

/**
 * 사용자가 고른 수집 아이템을 기기에서 바로 지운다. 한 건 밀기와 앱 묶음 지우기가 함께 쓴다.
 *
 * 보존 기간을 기다리지 않고 지우며, 지운 자연키는 다시 수집되지 않는다.
 */
class DeleteSourceItemsUseCase
    @Inject
    constructor(
        private val repository: SourceItemDeletionRepository,
    ) {
        /** @return 실제로 지운 아이템. 실행 취소에 쓴다. */
        suspend operator fun invoke(rawIds: Collection<String>): List<SourceItem> = repository.delete(rawIds)
    }
