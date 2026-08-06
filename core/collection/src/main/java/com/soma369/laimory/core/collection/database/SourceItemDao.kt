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

    /** rawId로 현재 행 전체를 읽는다. 읽기-수정-쓰기 트랜잭션에서 사용한다. */
    @Query("SELECT * FROM source_item WHERE rawId = :rawId LIMIT 1")
    suspend fun findByRawId(rawId: String): SourceItemEntity?

    /** 자연키로 현재 행 전체를 읽는다. 위치 저장 경로가 로컬 전용 STAY 필드를 보존할 때 사용한다. */
    @Query(
        "SELECT * FROM source_item WHERE itemType = :itemType AND sourceName = :sourceName AND sourceKey = :sourceKey LIMIT 1",
    )
    suspend fun findByNaturalKey(
        itemType: String,
        sourceName: String,
        sourceKey: String,
    ): SourceItemEntity?

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

    /**
     * 단일 시점 이벤트는 startAt, 구간 이벤트는 반열린 구간의 endAt을 기준으로 만료 행을 삭제한다.
     * cutoff와 같은 시점에 시작한 단일 이벤트와 cutoff를 걸치는 구간 이벤트는 유지한다.
     */
    @Query(
        "DELETE FROM source_item " +
            "WHERE (endAtUtc IS NULL AND startAtUtc < :cutoffUtc) " +
            "OR (endAtUtc IS NOT NULL AND endAtUtc <= :cutoffUtc)",
    )
    suspend fun deleteExpired(cutoffUtc: Long): Int

    @Query("DELETE FROM source_item WHERE itemType = :itemType")
    suspend fun deleteByItemType(itemType: String)
}
