package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.repository.SourceItemRepository

/** 저장된 사진(PHOTO) 아이템을 모두 비운다(스테이징 일괄 삭제). */
class ClearCollectedPhotosUseCase(
    private val repository: SourceItemRepository,
) {
    suspend operator fun invoke() = repository.clear(ItemType.PHOTO)
}
