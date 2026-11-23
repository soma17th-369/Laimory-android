package com.soma369.laimory.core.domain.model.analytics

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
import com.soma369.laimory.core.domain.model.timeline.DraftTaskFailureReason
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.SocketTimeoutException

class AnalyticsFailureCodeTest {
    @Test
    fun `사진 크기·개수·형식 한도 위반은 입력 오류로 가른다`() {
        listOf(-1004, -1005, -1007).forEach { code ->
            assertEquals(AnalyticsFailureCode.INVALID_INPUT, AnalyticsFailureCode.from(ApiException.ClientException(errorCode = code)))
        }
    }

    @Test
    fun `다른 4xx 는 원인을 단정하지 않는다`() {
        assertEquals(AnalyticsFailureCode.UNKNOWN, AnalyticsFailureCode.from(ApiException.ClientException(errorCode = -400)))
    }

    @Test
    fun `공통 안내 뒤 감싸 온 서버 오류는 열어서 서버 오류로 본다`() {
        assertEquals(AnalyticsFailureCode.SERVER, AnalyticsFailureCode.from(HandledException(ApiException.ServerException())))
    }

    @Test
    fun `연결·시간 초과·인증을 가른다`() {
        assertEquals(AnalyticsFailureCode.NETWORK, AnalyticsFailureCode.from(ApiException.NetworkException()))
        assertEquals(AnalyticsFailureCode.TIMEOUT, AnalyticsFailureCode.from(SocketTimeoutException()))
        assertEquals(AnalyticsFailureCode.AUTH, AnalyticsFailureCode.from(ApiException.UnauthorizedException()))
    }

    @Test
    fun `생성 실패 사유 중 기록 부족만 입력 부족으로 가르고 나머지는 서버 실패로 본다`() {
        assertEquals(AnalyticsFailureCode.INSUFFICIENT_EVENT, AnalyticsFailureCode.from(DraftTaskFailureReason.NO_EVENTS))
        listOf(
            DraftTaskFailureReason.AI_REPORTED_FAILURE,
            DraftTaskFailureReason.AI_DISPATCH_FAILURE,
            DraftTaskFailureReason.FINALIZE_FAILURE,
        ).forEach { reason ->
            assertEquals(AnalyticsFailureCode.SERVER, AnalyticsFailureCode.from(reason))
        }
        assertEquals(AnalyticsFailureCode.UNKNOWN, AnalyticsFailureCode.from(DraftTaskFailureReason.UNKNOWN))
    }
}
