package com.soma369.laimory.core.collection.database

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.soma369.laimory.core.collection.mapper.toDomain
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.StayPayload
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

/**
 * v1 → v2 는 스키마가 아니라 저장된 값(itemType/sourceKey)을 치환하므로
 * 스키마 검증만으로는 회귀를 못 잡는다 — 실제 v1 데이터를 넣고 변환 결과를 고정한다.
 */
@RunWith(AndroidJUnit4::class)
internal class CollectionDatabaseMigrationsTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            CollectionDatabase::class.java,
        )

    @Before
    fun clearDatabaseFile() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DB)
    }

    @Test
    fun v1의_LOCATION_행은_itemType과_sourceKey가_STAY로_전환된다() {
        helper.createDatabase(TEST_DB, 1).apply {
            insertV1Row(rawId = "raw-stay", itemType = "LOCATION", payloadJson = STAY_PAYLOAD, sourceKey = "LOCATION:1000")
            insertV1Row(rawId = "raw-move", itemType = "MOVEMENT", payloadJson = MOVE_PAYLOAD, sourceKey = "MOVEMENT:1000")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        db.query("SELECT itemType, sourceKey, payloadJson FROM source_item WHERE rawId = 'raw-stay'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("STAY", cursor.getString(0))
            assertEquals("STAY:1000", cursor.getString(1))
            // payload 는 변환 대상이 아니다 — 그대로 보존돼야 한다.
            assertEquals(STAY_PAYLOAD, cursor.getString(2))
        }
    }

    @Test
    fun 다른_itemType_행은_변환되지_않는다() {
        helper.createDatabase(TEST_DB, 1).apply {
            insertV1Row(rawId = "raw-move", itemType = "MOVEMENT", payloadJson = MOVE_PAYLOAD, sourceKey = "MOVEMENT:1000")
            insertV1Row(rawId = "raw-photo", itemType = "PHOTO", payloadJson = PHOTO_PAYLOAD, sourceKey = "PHOTO:1000")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        db.query("SELECT rawId, itemType, sourceKey FROM source_item ORDER BY rawId").use { cursor ->
            cursor.moveToFirst()
            assertEquals("raw-move", cursor.getString(0))
            assertEquals("MOVEMENT", cursor.getString(1))
            assertEquals("MOVEMENT:1000", cursor.getString(2))
            cursor.moveToNext()
            assertEquals("raw-photo", cursor.getString(0))
            assertEquals("PHOTO", cursor.getString(1))
            assertEquals("PHOTO:1000", cursor.getString(2))
        }
    }

    @Test
    fun 마이그레이션_후_DAO와_도메인_매핑으로_읽힌다() =
        runTest {
            helper.createDatabase(TEST_DB, 1).apply {
                insertV1Row(rawId = "raw-stay", itemType = "LOCATION", payloadJson = STAY_PAYLOAD, sourceKey = "LOCATION:1000")
                close()
            }

            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val database =
                Room
                    .databaseBuilder(context, CollectionDatabase::class.java, TEST_DB)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
            try {
                // 미변환 행이 남아 있으면 ItemType.valueOf("LOCATION") 에서 실패한다.
                val items = database.sourceItemDao().observeAll().first().map { it.toDomain() }
                val stay = items.single()
                assertEquals("raw-stay", stay.rawId)
                assertEquals(ItemType.STAY, stay.itemType)
                assertEquals(StayPayload(latitude = 37.5, longitude = 127.0), stay.payload)
            } finally {
                database.close()
            }
        }

    @Test
    fun v2에서_v3로_이동하면_진행_중_위치_테이블이_추가되고_기존_수집값은_유지된다() {
        helper.createDatabase(TEST_DB, 2).apply {
            insertV1Row(rawId = "raw-stay", itemType = "STAY", payloadJson = STAY_PAYLOAD, sourceKey = "STAY:1000")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        db.query("SELECT rawId FROM source_item WHERE rawId = 'raw-stay'").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
        }
        db.execSQL(
            "INSERT INTO ongoing_location_segment " +
                "(id, snapshotVersion, snapshotJson, updatedAtUtc) VALUES (1, 1, '{}', 3000)",
        )
        db.query("SELECT snapshotVersion, updatedAtUtc FROM ongoing_location_segment WHERE id = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
            assertEquals(3000L, cursor.getLong(1))
        }
    }

    @Test
    fun v3에서_v4로_이동하면_종일_일정은_기기_시간대_자정으로_정정되고_키는_유지된다() {
        // 15일 종일: 캘린더 원본 15일 00:00 UTC ~ 16일 00:00 UTC
        val allDayBegin = Instant.parse("2026-10-15T00:00:00Z").toEpochMilli()
        val allDayEnd = Instant.parse("2026-10-16T00:00:00Z").toEpochMilli()
        // 싱가포르 시간대 시간 지정 일정 03:30 ~ 05:00 UTC
        val timedBegin = Instant.parse("2026-10-16T03:30:00Z").toEpochMilli()
        val timedEnd = Instant.parse("2026-10-16T05:00:00Z").toEpochMilli()
        helper.createDatabase(TEST_DB, 3).apply {
            insertCalendarRow("raw-all-day", allDayBegin, allDayEnd, "UTC", ALL_DAY_PAYLOAD, "1:$allDayBegin")
            insertCalendarRow("raw-timed", timedBegin, timedEnd, "Asia/Singapore", TIMED_PAYLOAD, "2:$timedBegin")
            insertV1Row(rawId = "raw-stay", itemType = "STAY", payloadJson = STAY_PAYLOAD, sourceKey = "STAY:1000")
            close()
        }

        val seoul = ZoneId.of("Asia/Seoul")
        val migration =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) = migrateCalendarTimes(db, seoul)
            }
        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, migration)

        val query = "SELECT rawId, startAtUtc, endAtUtc, timeZoneId, sourceKey FROM source_item ORDER BY rawId"
        db.query(query).use { cursor ->
            cursor.moveToFirst()
            assertEquals("raw-all-day", cursor.getString(0))
            assertEquals(Instant.parse("2026-10-14T15:00:00Z").toEpochMilli(), cursor.getLong(1))
            assertEquals(Instant.parse("2026-10-15T15:00:00Z").toEpochMilli(), cursor.getLong(2))
            assertEquals("Asia/Seoul", cursor.getString(3))
            assertEquals("새 수집분과 같은 키라야 중복 저장되지 않는다", "1:$allDayBegin", cursor.getString(4))

            cursor.moveToNext()
            assertEquals("raw-stay", cursor.getString(0))
            assertEquals("다른 itemType 은 그대로", "Asia/Seoul", cursor.getString(3))
            assertEquals(1000L, cursor.getLong(1))

            cursor.moveToNext()
            assertEquals("raw-timed", cursor.getString(0))
            assertEquals("시간 지정 일정 시각은 그대로", timedBegin, cursor.getLong(1))
            assertEquals(timedEnd, cursor.getLong(2))
            assertEquals("Asia/Seoul", cursor.getString(3))
        }
    }

    private fun SupportSQLiteDatabase.insertCalendarRow(
        rawId: String,
        startAtUtc: Long,
        endAtUtc: Long,
        timeZoneId: String,
        payloadJson: String,
        sourceKey: String,
    ) {
        execSQL(
            "INSERT INTO source_item " +
                "(rawId, itemType, startAtUtc, endAtUtc, timeZoneId, payloadJson, sourceName, sourceKey, collectedAtUtc) " +
                "VALUES (?, 'CALENDAR', ?, ?, ?, ?, 'CALENDAR_PROVIDER', ?, 3000)",
            arrayOf(rawId, startAtUtc, endAtUtc, timeZoneId, payloadJson, sourceKey),
        )
    }

    private fun SupportSQLiteDatabase.insertV1Row(
        rawId: String,
        itemType: String,
        payloadJson: String,
        sourceKey: String,
    ) {
        execSQL(
            "INSERT INTO source_item " +
                "(rawId, itemType, startAtUtc, endAtUtc, timeZoneId, payloadJson, sourceName, sourceKey, collectedAtUtc) " +
                "VALUES (?, ?, 1000, 2000, 'Asia/Seoul', ?, 'LOCATION_PROVIDER', ?, 3000)",
            arrayOf(rawId, itemType, payloadJson, sourceKey),
        )
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
        const val STAY_PAYLOAD = """{"latitude":37.5,"longitude":127.0}"""
        const val MOVE_PAYLOAD =
            """{"start":{"latitude":37.5,"longitude":127.0},"end":{"latitude":37.6,"longitude":127.1},""" +
                """"distanceMeters":10.0,"transports":"WALKING"}"""
        const val ALL_DAY_PAYLOAD = """{"title":"휴가","description":null,"locationText":null,"allDay":true}"""
        const val TIMED_PAYLOAD = """{"title":"점심","description":null,"locationText":null,"allDay":false}"""
        const val PHOTO_PAYLOAD =
            """{"filename":"a.jpg","clientPhotoUri":"content://a","latitude":null,"longitude":null,"description":null}"""
    }
}
