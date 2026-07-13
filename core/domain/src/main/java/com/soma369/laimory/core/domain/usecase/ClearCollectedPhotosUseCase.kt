package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.repository.SourceItemRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 저장된 사진(PHOTO) 아이템을 모두 비운다(스테이징 일괄 삭제). */
@Singleton
class ClearCollectedPhotosUseCase
    @Inject
    constructor(
        private val repository: SourceItemRepository,
    ) {
        suspend operator fun invoke() = repository.clear(ItemType.PHOTO)
    }
