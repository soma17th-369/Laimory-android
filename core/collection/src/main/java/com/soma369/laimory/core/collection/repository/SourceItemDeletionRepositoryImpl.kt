package com.soma369.laimory.core.collection.repository

import com.soma369.laimory.core.collection.database.SourceItemDao
import com.soma369.laimory.core.collection.mapper.toDomain
import com.soma369.laimory.core.collection.mapper.toEntity
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.repository.SourceItemDeletionRepository
import java.time.Clock
import javax.inject.Inject

internal class SourceItemDeletionRepositoryImpl
    @Inject
    constructor(
        private val sourceItemDao: SourceItemDao,
        private val clock: Clock,
    ) : SourceItemDeletionRepository {
        override suspend fun delete(rawIds: Collection<String>): List<SourceItem> =
            sourceItemDao
                .deleteWithTombstones(rawIds = rawIds, deletedAtUtc = clock.millis())
                .map { entity -> entity.toDomain() }

        override suspend fun restore(items: List<SourceItem>) = sourceItemDao.restore(items.map(SourceItem::toEntity))
    }
