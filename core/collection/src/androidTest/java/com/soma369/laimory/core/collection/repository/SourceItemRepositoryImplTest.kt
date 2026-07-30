package com.soma369.laimory.core.collection.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.soma369.laimory.core.collection.database.CollectionDatabase
import com.soma369.laimory.core.domain.model.collection.NotificationPayload
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceName
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
internal class SourceItemRepositoryImplTest {
    private lateinit var database: CollectionDatabase
    private lateinit var repository: SourceItemRepositoryImpl

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
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun 같은_알림이_게시_후_클릭되어도_최초_수집_한_건만_유지된다() =
        runTest {
            val posted = notificationItem(rawId = "raw-posted", reason = NotificationPayload.CollectReason.KEYWORD)
            val clicked = notificationItem(rawId = "raw-clicked", reason = NotificationPayload.CollectReason.CLICK)

            assertEquals(1, repository.addAll(listOf(posted)))
            assertEquals(0, repository.addAll(listOf(clicked)))

            val stored = repository.observeAll().first().single()
            assertEquals("raw-posted", stored.rawId)
            assertEquals(
                NotificationPayload.CollectReason.KEYWORD,
                (stored.payload as NotificationPayload).collectReason,
            )
        }

    private fun notificationItem(
        rawId: String,
        reason: NotificationPayload.CollectReason,
    ): SourceItem =
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
                    collectReason = reason,
                ),
            sourceName = SourceName.NOTIFICATION_LISTENER,
            sourceKey = NOTIFICATION_SOURCE_KEY,
            collectedAt = EVENT_TIME,
        )

    private companion object {
        val EVENT_TIME: Instant = Instant.parse("2026-07-30T10:00:00Z")
        const val NOTIFICATION_SOURCE_KEY = "notification-key:1785405600000:com.example.app"
    }
}
