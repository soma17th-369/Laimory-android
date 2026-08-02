package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.SourceItemRetentionPolicy
import com.soma369.laimory.core.domain.repository.SourceItemRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 현재 날짜의 보존 경계를 계산해 만료된 로컬 수집 아이템을 삭제한다. */
@Singleton
class DeleteExpiredSourceItemsUseCase
    @Inject
    constructor(
        private val repository: SourceItemRepository,
        private val retentionPolicy: SourceItemRetentionPolicy,
    ) {
        /** @return 삭제된 아이템 수 */
        suspend operator fun invoke(): Int = repository.deleteExpired(retentionPolicy.cutoff())
    }
