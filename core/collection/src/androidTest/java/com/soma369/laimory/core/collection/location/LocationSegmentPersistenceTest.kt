package com.soma369.laimory.core.collection.location

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.soma369.laimory.core.collection.database.CollectionDatabase
import com.soma369.laimory.core.collection.mapper.toDomain
import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.model.collection.MovementPayload
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

    @Test
    fun 주소를_저장한_열린_STAY가_다시_갱신되어도_주소를_유지한다() =
        runTest {
            persistence.persist(createSnapshot(20 * MINUTE), listOf(createStay(20 * MINUTE)))
            val addressRepository =
                RoomLocationAddressRepository(
                    database = database,
                    sourceItemDao = database.sourceItemDao(),
                )
            addressRepository.updateAddress("stay-raw", "경기도 오산시")

            // 위치 수집기가 주소 없는 최신 구간을 저장해도 기존 로컬 주소를 이어받아야 한다.
            persistence.persist(createSnapshot(25 * MINUTE), listOf(createStay(25 * MINUTE)))

            val stored = database.sourceItemDao().observeAll().first().single().toDomain()
            assertEquals(Instant.ofEpochMilli(25 * MINUTE), stored.endAt)
            assertEquals("경기도 오산시", (stored.payload as StayPayload).address)

            // 반대 순서에서도 주소 쓰기가 최신 열린 구간의 시각을 되돌리지 않아야 한다.
            addressRepository.updateAddress("stay-raw", "경기도 오산시 세교동")
            val addressUpdated = database.sourceItemDao().observeAll().first().single().toDomain()
            assertEquals(Instant.ofEpochMilli(25 * MINUTE), addressUpdated.endAt)
            assertEquals("경기도 오산시 세교동", (addressUpdated.payload as StayPayload).address)
        }

    @Test
    fun 이동_출발과_도착_주소도_로컬에_저장하고_재수집에서_유지한다() =
        runTest {
            val movement = createMovement()
            persistence.persist(snapshot = null, items = listOf(movement))
            val addressRepository = RoomLocationAddressRepository(database, database.sourceItemDao())

            addressRepository.updateAddresses("move-raw", "서울특별시 마포구", "서울특별시 강남구")
            persistence.persist(snapshot = null, items = listOf(movement))

            val stored = database.sourceItemDao().observeAll().first().single().toDomain()
            val payload = stored.payload as MovementPayload
            assertEquals("서울특별시 마포구", payload.start.address)
            assertEquals("서울특별시 강남구", payload.end.address)
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

    private fun createMovement(): SourceItem =
        SourceItem(
            rawId = "move-raw",
            startAt = Instant.EPOCH,
            endAt = Instant.ofEpochMilli(20 * MINUTE),
            timeZoneId = ZoneId.of("Asia/Seoul"),
            payload =
                MovementPayload(
                    start = GeoPoint(37.5, 126.9),
                    end = GeoPoint(37.6, 127.0),
                    distanceMeters = 1_000.0,
                    transports = MovementPayload.Transport.IN_VEHICLE,
                ),
            sourceName = SourceName.LOCATION_PROVIDER,
            sourceKey = "MOVEMENT:0",
            collectedAt = Instant.ofEpochMilli(20 * MINUTE),
        )

    private companion object {
        const val MINUTE = 60_000L
    }
}
