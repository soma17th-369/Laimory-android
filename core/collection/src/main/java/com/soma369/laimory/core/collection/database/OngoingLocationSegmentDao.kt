package com.soma369.laimory.core.collection.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface OngoingLocationSegmentDao {
    @Query("SELECT * FROM ongoing_location_segment WHERE id = ${OngoingLocationSegmentEntity.SINGLETON_ID}")
    suspend fun get(): OngoingLocationSegmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OngoingLocationSegmentEntity)

    @Query("DELETE FROM ongoing_location_segment")
    suspend fun clear()
}
