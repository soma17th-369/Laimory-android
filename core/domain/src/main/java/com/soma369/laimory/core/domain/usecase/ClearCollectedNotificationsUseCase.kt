package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.repository.SourceItemRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 스테이징된 알림을 모두 비운다(일괄 삭제). */
@Singleton
class ClearCollectedNotificationsUseCase
    @Inject
    constructor(
        private val repository: SourceItemRepository,
    ) {
        suspend operator fun invoke() = repository.clear(ItemType.NOTIFICATION)
    }
