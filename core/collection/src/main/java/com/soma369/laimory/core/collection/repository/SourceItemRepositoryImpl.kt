package com.soma369.laimory.core.collection.repository

import com.soma369.laimory.core.collection.database.SourceItemDao
import com.soma369.laimory.core.collection.mapper.toDomain
import com.soma369.laimory.core.collection.mapper.toEntity
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.repository.SourceItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

internal class SourceItemRepositoryImpl
    @Inject
    constructor(
        private val sourceItemDao: SourceItemDao,
    ) : SourceItemRepository {
        override suspend fun addAll(items: List<SourceItem>): Int = sourceItemDao.addAll(items.map(SourceItem::toEntity))

        override suspend fun upsertAll(items: List<SourceItem>): Int = sourceItemDao.upsertAll(items.map(SourceItem::toEntity))

        override fun observeAll(): Flow<List<SourceItem>> =
            sourceItemDao.observeAll().map { entities ->
                entities.map { entity -> entity.toDomain() }
            }

        override suspend fun getInWindow(
            start: Instant,
            end: Instant,
        ): List<SourceItem> = sourceItemDao.getInWindow(start.toEpochMilli(), end.toEpochMilli()).map { it.toDomain() }

        override suspend fun getLatestCollectedAt(itemType: ItemType): Instant? =
            sourceItemDao.latestCollectedAtUtc(itemType.name)?.let(Instant::ofEpochMilli)

        override suspend fun deleteExpired(cutoff: Instant): Int {
            // 묘비도 같은 경계로 걷는다. 원래 행이 만료될 때쯤이면 같은 키가 다시 수집될 일도 없다.
            sourceItemDao.deleteExpiredTombstones(cutoff.toEpochMilli())
            return sourceItemDao.deleteExpired(cutoff.toEpochMilli())
        }

        override suspend fun clear(itemType: ItemType) = sourceItemDao.deleteByItemType(itemType.name)
    }
