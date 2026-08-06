package com.soma369.laimory.core.collection.location

import androidx.room.withTransaction
import com.soma369.laimory.core.collection.database.CollectionDatabase
import com.soma369.laimory.core.collection.database.OngoingLocationSegmentDao
import com.soma369.laimory.core.collection.database.OngoingLocationSegmentEntity
import com.soma369.laimory.core.collection.database.SourceItemDao
import com.soma369.laimory.core.collection.mapper.toDomain
import com.soma369.laimory.core.collection.mapper.toEntity
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.StayPayload
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
                if (items.isNotEmpty()) {
                    // 열린 STAY 갱신은 로컬에서 해석한 주소가 없는 원천 payload로 반복된다.
                    // 같은 트랜잭션에서 기존 주소를 이어받아 주소 갱신과의 lost update를 막는다.
                    val addressPreservedItems = items.map { item -> item.preserveStoredLocationAddress() }
                    sourceItemDao.upsertAll(addressPreservedItems.map(SourceItem::toEntity))
                }
                if (snapshot == null) {
                    ongoingDao.clear()
                } else {
                    ongoingDao.upsert(snapshot.toEntity())
                }
            }
        }

        override suspend fun awaitIdle() = mutex.withLock { Unit }

        private suspend fun SourceItem.preserveStoredLocationAddress(): SourceItem {
            val stored =
                sourceItemDao.findByNaturalKey(
                    itemType = itemType.name,
                    sourceName = sourceName.name,
                    sourceKey = sourceKey,
                )?.toDomain() ?: return this
            return when (val incoming = payload) {
                is StayPayload -> {
                    if (!incoming.address.isNullOrBlank()) return this
                    val storedAddress = (stored.payload as? StayPayload)?.address?.takeIf(String::isNotBlank) ?: return this
                    copy(payload = incoming.copy(address = storedAddress))
                }
                is MovementPayload -> {
                    val storedPayload = stored.payload as? MovementPayload ?: return this
                    val start =
                        if (incoming.start.address.isNullOrBlank()) {
                            incoming.start.copy(address = storedPayload.start.address)
                        } else {
                            incoming.start
                        }
                    val end =
                        if (incoming.end.address.isNullOrBlank()) {
                            incoming.end.copy(address = storedPayload.end.address)
                        } else {
                            incoming.end
                        }
                    copy(payload = incoming.copy(start = start, end = end))
                }
                else -> this
            }
        }

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
