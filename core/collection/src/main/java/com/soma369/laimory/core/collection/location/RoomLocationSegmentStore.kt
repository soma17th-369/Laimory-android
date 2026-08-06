package com.soma369.laimory.core.collection.location

import androidx.room.withTransaction
import com.soma369.laimory.core.collection.database.CollectionDatabase
import com.soma369.laimory.core.collection.database.OngoingLocationSegmentDao
import com.soma369.laimory.core.collection.database.OngoingLocationSegmentEntity
import com.soma369.laimory.core.collection.database.SourceItemDao
import com.soma369.laimory.core.collection.mapper.toEntity
import com.soma369.laimory.core.domain.model.collection.SourceItem
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Room 트랜잭션으로 [LocationSegmentStore]를 구현한다. */
@Singleton
internal class RoomLocationSegmentStore
    @Inject
    constructor(
        private val database: CollectionDatabase,
        private val ongoingDao: OngoingLocationSegmentDao,
        private val sourceItemDao: SourceItemDao,
    ) : LocationSegmentStore {
        private val mutex = Mutex()

        override suspend fun restore(): LocationSegmentSnapshot? =
            mutex.withLock {
                val entity = ongoingDao.get() ?: return@withLock null
                if (entity.snapshotVersion != SNAPSHOT_VERSION) {
                    ongoingDao.clear()
                    return@withLock null
                }
                runCatching { LocationSegmentSnapshotCodec.decode(entity.snapshotJson) }
                    .getOrElse {
                        ongoingDao.clear()
                        null
                    }
            }

        override suspend fun persist(
            snapshot: LocationSegmentSnapshot?,
            items: List<SourceItem>,
        ) = mutex.withLock {
            database.withTransaction {
                if (items.isNotEmpty()) sourceItemDao.upsertAll(items.map(SourceItem::toEntity))
                if (snapshot == null) {
                    ongoingDao.clear()
                } else {
                    ongoingDao.upsert(snapshot.toEntity())
                }
            }
        }

        override suspend fun awaitIdle() = mutex.withLock { Unit }

        private fun LocationSegmentSnapshot.toEntity(): OngoingLocationSegmentEntity =
            OngoingLocationSegmentEntity(
                snapshotVersion = SNAPSHOT_VERSION,
                snapshotJson = LocationSegmentSnapshotCodec.encode(this),
                updatedAtUtc = previousSample.timeMillis,
            )

        private companion object {
            const val SNAPSHOT_VERSION = 1
        }
    }
