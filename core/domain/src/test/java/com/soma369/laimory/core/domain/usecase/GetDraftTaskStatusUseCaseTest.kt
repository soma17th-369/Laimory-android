package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.DraftTaskHandle
import com.soma369.laimory.core.domain.model.timeline.DraftTaskSnapshot
import com.soma369.laimory.core.domain.model.timeline.DraftTaskStatusOutcome
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import com.soma369.laimory.core.domain.repository.TimelineDraftRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GetDraftTaskStatusUseCaseTest {
    private class ThrowingRepository(
        private val exception: ApiException,
    ) : TimelineDraftRepository {
        override suspend fun getDraftStatus(taskId: String): DraftTaskSnapshot = throw exception

        override suspend fun uploadPhotos(clientPhotoUris: List<String>): List<String> = error("사용하지 않음")

        override suspend fun createDraft(
            recordDate: LocalDate,
            zone: ZoneId,
            window: RecordDateWindow,
            items: List<SourceItem>,
            uploadedPhotoFilenames: Map<String, String>,
        ): DraftTaskHandle = error("사용하지 않음")
    }

    private class RecordingMessageHelper : MessageHelper {
        val messages = mutableListOf<UserMessage>()

        override fun send(message: UserMessage) {
            messages += message
        }
    }

    @Test
    fun `ERROR_1001은 작업 소멸 outcome으로 변환한다`() =
        runBlocking {
            val helper = RecordingMessageHelper()
            val useCase =
                GetDraftTaskStatusUseCase(
                    ThrowingRepository(ApiException.ClientException(errorCode = "ERROR_1001", rawCode = 404)),
                    helper,
                )

            val result = useCase("task-1")

            assertEquals(DraftTaskStatusOutcome.TaskUnavailable, result.getOrNull())
            assertTrue(helper.messages.isEmpty())
        }

    @Test
    fun `ERROR_0404는 결과 소멸 outcome으로 변환한다`() =
        runBlocking {
            val helper = RecordingMessageHelper()
            val useCase =
                GetDraftTaskStatusUseCase(
                    ThrowingRepository(ApiException.ClientException(errorCode = "ERROR_0404", rawCode = 404)),
                    helper,
                )

            val result = useCase("task-1")

            assertEquals(DraftTaskStatusOutcome.ResultUnavailable, result.getOrNull())
            assertTrue(helper.messages.isEmpty())
        }

    @Test
    fun `미지 404는 기존 BaseUseCase 공통 정책으로 처리한다`() =
        runBlocking {
            val helper = RecordingMessageHelper()
            val useCase =
                GetDraftTaskStatusUseCase(
                    ThrowingRepository(ApiException.ClientException(errorCode = "ERROR_1999", rawCode = 404)),
                    helper,
                )

            val result = useCase("task-1")

            assertTrue(result.exceptionOrNull() is HandledException)
            assertEquals(listOf(UserMessage.UnsupportedFeature), helper.messages)
        }
}
