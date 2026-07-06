package com.soma369.laimory.core.collection.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SourceItemDao {
    /**
     * 아이템을 저장한다. `sourceKey` UNIQUE 충돌 시 기존 행을 유지하고 무시한다.
     *
     * @return 저장된 rowId 목록. 무시된 아이템은 -1.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(entities: List<SourceItemEntity>): List<Long>

    @Query("SELECT * FROM source_item ORDER BY startAtUtc DESC")
    fun observeAll(): Flow<List<SourceItemEntity>>

    @Query("SELECT MAX(collectedAtUtc) FROM source_item WHERE itemType = :itemType")
    suspend fun latestCollectedAtUtc(itemType: String): Long?
}
