package com.soma369.laimory.core.collection.location

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.soma369.laimory.core.collection.database.CollectionDatabase
import com.soma369.laimory.core.collection.mapper.toDomain
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.collection.StayPayload
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
internal class LocationSegmentPersistenceTest {
    private lateinit var database: CollectionDatabase
    private lateinit var persistence: RoomLocationSegmentStore

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    CollectionDatabase::class.java,
                ).build()
        persistence =
            RoomLocationSegmentStore(
                database = database,
                ongoingDao = database.ongoingLocationSegmentDao(),
                sourceItemDao = database.sourceItemDao(),
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun 진행_상태와_확정_STAY를_함께_저장하고_복원한다() =
        runTest {
            val snapshot = createSnapshot(endMillis = 20 * MINUTE)
            val stay = createStay(endMillis = 20 * MINUTE)

            persistence.persist(snapshot, listOf(stay))

            assertEquals(snapshot, persistence.restore())
            val stored = database.sourceItemDao().observeAll().first().single().toDomain()
            assertEquals(stay, stored)
        }

    @Test
    fun 체류_마감_트랜잭션은_진행_상태를_지우고_같은_rawId의_STAY를_갱신한다() =
        runTest {
            persistence.persist(createSnapshot(20 * MINUTE), listOf(createStay(20 * MINUTE)))

            persistence.persist(snapshot = null, items = listOf(createStay(25 * MINUTE)))

            assertNull(persistence.restore())
            val stored = database.sourceItemDao().observeAll().first().single().toDomain()
            assertEquals("stay-raw", stored.rawId)
            assertEquals(Instant.ofEpochMilli(25 * MINUTE), stored.endAt)
        }

    private fun createSnapshot(endMillis: Long): LocationSegmentSnapshot {
        val start = LocationSample(37.5, 127.0, 0L, 20.0)
        val end = LocationSample(37.5, 127.0, endMillis, 20.0)
        val place = PlaceAccumulator.from("stay-raw", start, minimumAccuracyMeters = 5.0).add(end, 5.0)
        return LocationSegmentSnapshot(
            state = LocationSegmentState.AtPlace(place = place, confirmed = true),
            previousSample = end,
        )
    }

    private fun createStay(endMillis: Long): SourceItem =
        SourceItem(
            rawId = "stay-raw",
            startAt = Instant.EPOCH,
            endAt = Instant.ofEpochMilli(endMillis),
            timeZoneId = ZoneId.of("Asia/Seoul"),
            payload = StayPayload(latitude = 37.5, longitude = 127.0),
            sourceName = SourceName.LOCATION_PROVIDER,
            sourceKey = "STAY:0",
            collectedAt = Instant.ofEpochMilli(endMillis),
        )

    private companion object {
        const val MINUTE = 60_000L
    }
}
