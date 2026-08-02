package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.collection.SourceItemRetentionConfig
import com.soma369.laimory.core.domain.model.collection.SourceItemRetentionPolicy
import com.soma369.laimory.core.domain.repository.SourceItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class DeleteExpiredSourceItemsUseCaseTest {
    @Test
    fun `계산한 날짜 경계로 repository 삭제를 요청하고 삭제 건수를 반환한다`() =
        runTest {
            val repository = RecordingSourceItemRepository(deletedCount = 7)
            val useCase =
                DeleteExpiredSourceItemsUseCase(
                    repository = repository,
                    retentionPolicy =
                        SourceItemRetentionPolicy(
                            config = SourceItemRetentionConfig(30),
                            clock = Clock.fixed(Instant.parse("2026-08-02T12:00:00Z"), ZoneId.of("UTC")),
                            zoneIdProvider = { ZoneId.of("Asia/Seoul") },
                        ),
                )

            assertEquals(7, useCase())
            assertEquals(Instant.parse("2026-07-03T15:00:00Z"), repository.requestedCutoff)
        }

    private class RecordingSourceItemRepository(
        private val deletedCount: Int,
    ) : SourceItemRepository {
        var requestedCutoff: Instant? = null

        override suspend fun addAll(items: List<SourceItem>): Int = error("사용하지 않음")

        override suspend fun upsertAll(items: List<SourceItem>): Int = error("사용하지 않음")

        override fun observeAll(): Flow<List<SourceItem>> = emptyFlow()

        override suspend fun getLatestCollectedAt(itemType: ItemType): Instant? = error("사용하지 않음")

        override suspend fun deleteExpired(cutoff: Instant): Int {
            requestedCutoff = cutoff
            return deletedCount
        }

        override suspend fun clear(itemType: ItemType) = error("사용하지 않음")
    }
}
