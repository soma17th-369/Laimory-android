package com.soma369.laimory.core.collection.location

import androidx.room.withTransaction
import com.soma369.laimory.core.collection.database.CollectionDatabase
import com.soma369.laimory.core.collection.database.SourceItemDao
import com.soma369.laimory.core.collection.mapper.toDomain
import com.soma369.laimory.core.collection.mapper.toEntity
import com.soma369.laimory.core.domain.model.collection.MovementPayload
import com.soma369.laimory.core.domain.model.collection.StayPayload
import com.soma369.laimory.core.domain.repository.MovementAddressRepository
import com.soma369.laimory.core.domain.repository.StayAddressRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 주소 갱신과 위치 수집 upsert의 lost update를 막기 위해 읽기-수정-쓰기를 한 Room 트랜잭션으로 수행한다. */
@Singleton
internal class RoomLocationAddressRepository
    @Inject
    constructor(
        private val database: CollectionDatabase,
        private val sourceItemDao: SourceItemDao,
    ) : StayAddressRepository,
        MovementAddressRepository {
        override suspend fun updateAddress(
            rawId: String,
            address: String,
        ): Boolean {
            val normalizedAddress = address.normalized() ?: return false
            return database.withTransaction {
                val entity = sourceItemDao.findByRawId(rawId) ?: return@withTransaction false
                val item = entity.toDomain()
                val payload = item.payload as? StayPayload ?: return@withTransaction false
                sourceItemDao.insertOrReplace(
                    listOf(item.copy(payload = payload.copy(address = normalizedAddress)).toEntity()),
                )
                true
            }
        }

        override suspend fun updateAddresses(
            rawId: String,
            startAddress: String?,
            endAddress: String?,
        ): Boolean {
            val normalizedStart = startAddress.normalized()
            val normalizedEnd = endAddress.normalized()
            if (normalizedStart == null && normalizedEnd == null) return false

            return database.withTransaction {
                val entity = sourceItemDao.findByRawId(rawId) ?: return@withTransaction false
                val item = entity.toDomain()
                val payload = item.payload as? MovementPayload ?: return@withTransaction false
                val updatedPayload =
                    payload.copy(
                        start = normalizedStart?.let { payload.start.copy(address = it) } ?: payload.start,
                        end = normalizedEnd?.let { payload.end.copy(address = it) } ?: payload.end,
                    )
                sourceItemDao.insertOrReplace(listOf(item.copy(payload = updatedPayload).toEntity()))
                true
            }
        }

        private fun String?.normalized(): String? = this?.trim()?.takeIf(String::isNotEmpty)
    }
