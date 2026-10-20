package com.soma369.laimory.core.domain.exception

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.message.UserMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerMessageTest {
    @Test
    fun `서버 문구가 있으면 대체 문구보다 먼저 쓴다`() {
        val error = ApiException.ClientException(errorCode = -1021, rawCode = 403, serverMessage = "크레딧이 부족해요.")

        assertEquals("크레딧이 부족해요.", error.serverMessageOr("앱 문구"))
    }

    @Test
    fun `서버 문구가 없으면 대체 문구를 쓴다`() {
        assertEquals("앱 문구", ApiException.NetworkException().serverMessageOr("앱 문구"))
        assertEquals("앱 문구", IllegalStateException("개발용").serverMessageOr("앱 문구"))
    }

    @Test
    fun `기능 예외로 감싼 원인에서도 서버 문구를 찾는다`() {
        val cause = ApiException.ConflictException(errorCode = -1016, rawCode = 409, serverMessage = "진행 중이에요.")
        val wrapped =
            HandledException(TimelineRecordDeleteException(TimelineRecordDeleteException.Reason.DATE_OPERATION_IN_PROGRESS, cause))

        assertEquals("진행 중이에요.", wrapped.findServerMessage())
    }

    @Test
    fun `공통 스낵바 문구는 개발용 예외 메시지를 내보이지 않는다`() {
        val parseFailure = ApiException.UnknownException("알 수 없는 초안 작업 상태입니다: WAITING")
        val springDefault = ApiException.fromCode(404, message = "Not Found")

        assertEquals(ApiException.UNKNOWN_ERROR, parseFailure.displayMessage)
        assertEquals(ApiException.CLIENT_ERROR, springDefault.displayMessage)
        assertEquals("서버 문구", ApiException.fromCode(500, serverMessage = "서버 문구").displayMessage)
    }

    @Test
    fun `공통 정책은 서버 문구가 있으면 서버 안내로 알린다`() =
        runBlocking {
            val helper = RecordingMessageHelper()
            val useCase = FailingUseCase(helper)

            val result = useCase(ApiException.fromCode(502, errorCode = -1008, serverMessage = "AI 타임라인 생성에 실패했어요."))

            assertEquals(listOf(UserMessage.ServerNotice("AI 타임라인 생성에 실패했어요.")), helper.messages)
            assertEquals(HandledException::class, result.exceptionOrNull()!!::class)
        }

    @Test
    fun `공통 정책은 서버 문구가 없으면 기존 안내로 알린다`() =
        runBlocking {
            val helper = RecordingMessageHelper()
            val useCase = FailingUseCase(helper)

            useCase(ApiException.fromCode(401))
            useCase(ApiException.fromCode(404))
            useCase(ApiException.fromCode(503))

            assertEquals(
                listOf(UserMessage.SessionExpired, UserMessage.UnsupportedFeature, UserMessage.TemporaryUnavailable),
                helper.messages,
            )
        }

    @Test
    fun `공통 정책 밖의 실패는 서버 문구가 있어도 화면에 넘긴다`() =
        runBlocking {
            val helper = RecordingMessageHelper()
            val error = ApiException.fromCode(409, errorCode = -1013, serverMessage = "이미 타임라인에 추가된 기록이에요.")

            val result = FailingUseCase(helper)(error)

            assertEquals(error, result.exceptionOrNull())
            assertNull(helper.messages.firstOrNull())
        }

    private class FailingUseCase(
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(error: ApiException): Result<Unit> = execute { throw error }
    }

    private class RecordingMessageHelper : MessageHelper {
        val messages = mutableListOf<UserMessage>()

        override fun send(message: UserMessage) {
            messages += message
        }
    }
}
