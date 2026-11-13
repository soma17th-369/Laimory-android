package com.soma369.laimory.core.collection.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.soma369.laimory.core.collection.database.CollectionDatabase
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import com.soma369.laimory.core.domain.model.collection.StayPayload
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
internal class SourceItemDeletionRepositoryImplTest {
    private lateinit var database: CollectionDatabase
    private lateinit var repository: SourceItemRepositoryImpl
    private lateinit var deletion: SourceItemDeletionRepositoryImpl

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    CollectionDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        repository = SourceItemRepositoryImpl(database.sourceItemDao())
        deletion = SourceItemDeletionRepositoryImpl(database.sourceItemDao(), Clock.fixed(NOW, ZoneOffset.UTC))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun 지운_알림은_같은_키로_다시_들어와도_저장되지_않는다() =
        runTest {
            repository.addAll(listOf(notification("raw-posted")))

            val deleted = deletion.delete(listOf("raw-posted"))
            // 게시 뒤 클릭 이벤트가 같은 sourceKey 로 다시 들어오는 경로다.
            val inserted = repository.addAll(listOf(notification("raw-clicked")))

            assertEquals(listOf("raw-posted"), deleted.map(SourceItem::rawId))
            assertEquals(0, inserted)
            assertTrue(repository.observeAll().first().isEmpty())
        }

    @Test
    fun 지운_진행_중_체류는_같은_키로_갱신돼도_되살아나지_않는다() =
        runTest {
            repository.upsertAll(listOf(stay(endAt = EVENT_TIME.plusSeconds(600))))

            deletion.delete(listOf("raw-stay"))
            repository.upsertAll(listOf(stay(endAt = EVENT_TIME.plusSeconds(1_200))))

            assertTrue(repository.observeAll().first().isEmpty())
        }

    @Test
    fun 되돌리면_지운_행이_그대로_돌아오고_다시_수집할_수_있다() =
        runTest {
            repository.upsertAll(listOf(stay(endAt = EVENT_TIME.plusSeconds(600))))
            val deleted = deletion.delete(listOf("raw-stay"))

            deletion.restore(deleted)
            repository.upsertAll(listOf(stay(endAt = EVENT_TIME.plusSeconds(1_200))))

            val stored = repository.observeAll().first().single()
            assertEquals("raw-stay", stored.rawId)
            assertEquals(EVENT_TIME.plusSeconds(1_200), stored.endAt)
        }

    @Test
    fun 없는_rawId_는_건너뛴다() =
        runTest {
            assertTrue(deletion.delete(listOf("missing")).isEmpty())
        }

    @Test
    fun 보존_기간이_지나면_묘비도_지워져_같은_키를_다시_받는다() =
        runTest {
            repository.addAll(listOf(notification("raw-posted")))
            deletion.delete(listOf("raw-posted"))

            repository.deleteExpired(EVENT_TIME.plusSeconds(1))

            assertEquals(1, repository.addAll(listOf(notification("raw-again"))))
        }

    private fun notification(rawId: String): SourceItem =
        SourceItem(
            rawId = rawId,
            startAt = EVENT_TIME,
            endAt = null,
            timeZoneId = ZoneId.of("Asia/Seoul"),
            payload =
                NotificationPayload(
                    appName = "테스트 앱",
                    packageName = "com.example.app",
                    title = "테스트 제목",
                    text = "테스트 본문",
                    collectReason = NotificationPayload.CollectReason.APP,
                ),
            sourceName = SourceName.NOTIFICATION_LISTENER,
            sourceKey = "notification-key",
            collectedAt = EVENT_TIME,
        )

    private fun stay(endAt: Instant): SourceItem =
        SourceItem(
            rawId = "raw-stay",
            startAt = EVENT_TIME,
            endAt = endAt,
            timeZoneId = ZoneId.of("Asia/Seoul"),
            payload = StayPayload(latitude = 37.5665, longitude = 126.9780, address = null),
            sourceName = SourceName.LOCATION_PROVIDER,
            sourceKey = "STAY:${EVENT_TIME.toEpochMilli()}",
            collectedAt = EVENT_TIME,
        )

    private companion object {
        val EVENT_TIME: Instant = Instant.parse("2026-11-01T03:00:00Z")
        val NOW: Instant = Instant.parse("2026-11-13T00:00:00Z")
    }
}
