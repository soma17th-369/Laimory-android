package com.soma369.laimory.core.collection.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entities: List<SourceItemEntity>)

    /** (itemType, sourceName, sourceKey) 로 기존 행의 stable rawId 를 찾는다. 없으면 null. */
    @Query(
        "SELECT rawId FROM source_item WHERE itemType = :itemType AND sourceName = :sourceName AND sourceKey = :sourceKey",
    )
    suspend fun findRawId(
        itemType: String,
        sourceName: String,
        sourceKey: String,
    ): String?

    /**
     * upsert. 기존 (itemType, sourceName, sourceKey) 행이 있으면 최초 [SourceItemEntity.rawId] 를 유지한 채
     * 값(payload·시각)을 갱신하고, 없으면 새로 삽입한다. 걸음수 일별 집계처럼 값이 변하는 aggregate 에 쓴다.
     * (불변 이벤트는 [insertOrIgnore] 를 그대로 쓴다.)
     *
     * @return 새로 삽입된(갱신 제외) 아이템 수.
     */
    @Transaction
    suspend fun upsertAll(entities: List<SourceItemEntity>): Int {
        var insertedCount = 0
        val resolved =
            entities.map { entity ->
                val existingRawId = findRawId(entity.itemType, entity.sourceName, entity.sourceKey)
                if (existingRawId == null) {
                    insertedCount++
                    entity
                } else {
                    // 기존 rawId 재사용 → PK 충돌로 REPLACE 되어 값만 in-place 갱신(rawId 안정성 보존).
                    entity.copy(rawId = existingRawId)
                }
            }
        insertOrReplace(resolved)
        return insertedCount
    }

    @Query("SELECT * FROM source_item ORDER BY startAtUtc DESC")
    fun observeAll(): Flow<List<SourceItemEntity>>

    @Query("SELECT MAX(collectedAtUtc) FROM source_item WHERE itemType = :itemType")
    suspend fun latestCollectedAtUtc(itemType: String): Long?

    @Query("DELETE FROM source_item WHERE itemType = :itemType")
    suspend fun deleteByItemType(itemType: String)
}
