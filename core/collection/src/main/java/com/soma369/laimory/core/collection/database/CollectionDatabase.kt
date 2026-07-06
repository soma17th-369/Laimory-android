package com.soma369.laimory.core.collection.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * 수집 영역 로컬 DB.
 *
 * 수집 데이터의 저장(Room)은 `:core:data` 의 remote 책임과 분리해
 * `:core:collection` 이 자체 소유한다. 스키마 변경과 migration 도 이 모듈이 관리한다.
 */
@Database(
    entities = [SourceItemEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class CollectionDatabase : RoomDatabase() {
    abstract fun sourceItemDao(): SourceItemDao

    companion object {
        const val NAME = "collection.db"
    }
}
