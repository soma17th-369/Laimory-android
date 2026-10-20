package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.DraftTaskHandle
import com.soma369.laimory.core.domain.model.timeline.DraftTaskSnapshot
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CheckTimelineCreatableUseCaseTest {
    @Test
    fun `서버가 만들 수 없다고 하면 false 다`() =
        runTest {
            assertFalse(CheckTimelineCreatableUseCase(FakeRepository { false })(emptyList(), ZONE))
        }

    @Test
    fun `서버가 만들 수 있다고 하면 true 다`() =
        runTest {
            assertTrue(CheckTimelineCreatableUseCase(FakeRepository { true })(emptyList(), ZONE))
        }

    @Test
    fun `아직 API 가 없는 서버(404)면 막지 않는다`() =
        runTest {
            val repository = FakeRepository { throw ApiException.ClientException(rawCode = 404) }

            assertTrue(CheckTimelineCreatableUseCase(repository)(emptyList(), ZONE))
        }

    @Test
    fun `끊기면 막지 않는다`() =
        runTest {
            assertTrue(CheckTimelineCreatableUseCase(FakeRepository { throw ApiException.NetworkException() })(emptyList(), ZONE))
        }

    @Test
    fun `3초 안에 못 받으면 막지 않는다`() =
        runTest {
            val repository =
                FakeRepository {
                    delay(5_000)
                    false
                }

            assertTrue(CheckTimelineCreatableUseCase(repository)(emptyList(), ZONE))
        }

    private companion object {
        val ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }

    private class FakeRepository(
        private val creatable: suspend () -> Boolean,
    ) : TimelineDraftRepository {
        override suspend fun isCreatable(
            items: List<SourceItem>,
            zone: ZoneId,
        ): Boolean = creatable()

        override suspend fun uploadPhotos(clientPhotoUris: List<String>): List<String> = error("사용하지 않음")

        override suspend fun createDraft(
            recordDate: LocalDate,
            zone: ZoneId,
            window: RecordDateWindow,
            items: List<SourceItem>,
            uploadedPhotoFilenames: Map<String, String>,
        ): DraftTaskHandle = error("사용하지 않음")

        override suspend fun getDraftStatus(taskId: String): DraftTaskSnapshot = error("사용하지 않음")
    }
}
