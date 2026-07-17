package com.soma369.laimory.core.collection.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: 체류 itemType 을 `LOCATION` 에서 서버 초안 계약과 같은 `STAY` 로 통일한다.
 *
 * `sourceKey` 의 `LOCATION:` 접두사도 함께 바꿔서 (itemType, sourceName, sourceKey)
 * 멱등 UNIQUE 키가 새 수집분과 같은 형태를 유지하게 한다(미변환 시 같은 체류가 중복 저장됨).
 */
internal val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("UPDATE source_item SET itemType = 'STAY' WHERE itemType = 'LOCATION'")
            // substr 시작 위치 10 = "LOCATION:" 접두사 길이 9 + 1
            db.execSQL(
                "UPDATE source_item SET sourceKey = 'STAY:' || substr(sourceKey, 10) " +
                    "WHERE itemType = 'STAY' AND sourceKey LIKE 'LOCATION:%'",
            )
        }
    }
