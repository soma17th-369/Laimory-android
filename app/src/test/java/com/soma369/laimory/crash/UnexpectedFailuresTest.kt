package com.soma369.laimory.crash

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class UnexpectedFailuresTest {
    @Test
    fun `뜻을 알 수 없었던 응답은 보고한다`() {
        // safeApiCall 은 역직렬화 실패와 body 누락을 이 타입으로 바꾼다. 서버가 계약을 바꿔 앱이
        // 조용히 깨지는 상황이 정확히 이 모양이라, 이것만은 놓치면 안 된다.
        assertTrue(isUnexpectedFailure(ApiException.UnknownException()))
    }

    @Test
    fun `서버가 알려 준 오류와 통신 사정은 보고하지 않는다`() {
        val expected =
            listOf(
                ApiException.NetworkException(),
                ApiException.UnauthorizedException(),
                ApiException.ClientException(),
                ApiException.ServerException(),
                ApiException.ConflictException(),
            )

        expected.forEach { assertFalse(it::class.simpleName, isUnexpectedFailure(it)) }
    }

    @Test
    fun `이미 다룬 실패와 취소는 보고하지 않는다`() {
        assertFalse(isUnexpectedFailure(HandledException(ApiException.ServerException())))
        assertFalse(isUnexpectedFailure(CancellationException("화면을 떠남")))
    }

    @Test
    fun `그 밖의 예외는 보고한다`() {
        assertTrue(isUnexpectedFailure(IllegalStateException("boom")))
        assertTrue(isUnexpectedFailure(NullPointerException()))
    }
}
