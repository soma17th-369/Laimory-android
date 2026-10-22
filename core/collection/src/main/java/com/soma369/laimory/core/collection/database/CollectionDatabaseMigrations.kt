package com.soma369.laimory.core.collection.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.soma369.laimory.core.collection.collector.CalendarAllDayPolicy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.ZoneId

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

/** v2 → v3: 프로세스 재시작 뒤 위치 분절을 이어갈 단일 진행 상태 테이블을 추가한다. */
internal val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS ongoing_location_segment (" +
                    "id INTEGER NOT NULL, " +
                    "snapshotVersion INTEGER NOT NULL, " +
                    "snapshotJson TEXT NOT NULL, " +
                    "updatedAtUtc INTEGER NOT NULL, " +
                    "PRIMARY KEY(id))",
            )
        }
    }

/**
 * v3 → v4: 저장된 캘린더 일정 시각을 캘린더 앱에 보이는 시각으로 정정한다(#504).
 *
 * 스키마는 그대로고 값만 바꾼다. 기기 시간대는 마이그레이션이 도는 시점의 값이다.
 */
internal val MIGRATION_3_4 =
    object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migrateCalendarTimes(db, ZoneId.systemDefault())
        }
    }

/**
 * [MIGRATION_3_4] 의 본체. 시간대를 받아 테스트에서 고정할 수 있게 분리했다.
 *
 * - 종일 일정: UTC 자정으로 저장된 시각을 [zone] 자정으로 바꾼다([CalendarAllDayPolicy]).
 * - 모든 캘린더 행: `timeZoneId` 를 [zone] 으로 맞춘다(새 수집분과 같은 기준).
 * - `sourceKey` 는 캘린더 원본 BEGIN 이라 손대지 않는다 — 새 수집분과 키가 같아 중복 저장되지 않는다.
 */
internal fun migrateCalendarTimes(
    db: SupportSQLiteDatabase,
    zone: ZoneId,
) {
    val allDayRows =
        db.query("SELECT rawId, startAtUtc, endAtUtc, payloadJson FROM source_item WHERE itemType = 'CALENDAR'").use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    if (!cursor.getString(3).isAllDayPayload()) continue
                    val endAtUtc = if (cursor.isNull(2)) null else cursor.getLong(2)
                    add(Triple(cursor.getString(0), cursor.getLong(1), endAtUtc))
                }
            }
        }
    allDayRows.forEach { (rawId, startAtUtc, endAtUtc) ->
        db.execSQL(
            "UPDATE source_item SET startAtUtc = ?, endAtUtc = ? WHERE rawId = ?",
            arrayOf<Any?>(
                CalendarAllDayPolicy.toZonedMidnight(startAtUtc, zone).toEpochMilli(),
                endAtUtc?.let { CalendarAllDayPolicy.toZonedMidnight(it, zone).toEpochMilli() },
                rawId,
            ),
        )
    }
    db.execSQL("UPDATE source_item SET timeZoneId = ? WHERE itemType = 'CALENDAR'", arrayOf<Any?>(zone.id))
}

/** 캘린더 payload 의 `allDay` 가 true 인지. 읽을 수 없는 payload 는 시간 지정 일정으로 보고 시각을 두지 않는다. */
private fun String.isAllDayPayload(): Boolean =
    runCatching {
        Json.parseToJsonElement(this).jsonObject["allDay"]?.jsonPrimitive?.booleanOrNull
    }.getOrNull() == true

/**
 * v4 → v5: 저장된 캘린더 일정을 지운다.
 *
 * 일정은 이제 저장하지 않고 볼 때마다 시스템 캘린더에서 읽는다. 남겨 두면 새로 읽은 일정과 겹쳐 두 번 세고
 * 두 번 보낸다 — 예전 행은 무작위 rawId 라 새로 읽은 것과 키가 다르다. 스키마는 그대로다.
 */
internal val MIGRATION_4_5 =
    object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DELETE FROM source_item WHERE itemType = 'CALENDAR'")
        }
    }
