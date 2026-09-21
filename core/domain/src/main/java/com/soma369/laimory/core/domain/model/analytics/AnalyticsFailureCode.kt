package com.soma369.laimory.core.domain.model.analytics

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.timeline.DraftTaskFailureReason
import java.net.SocketTimeoutException

/**
 * 실패를 정해진 값으로만 보낸다. 예외 메시지는 원문·경로가 섞일 수 있어 보내지 않는다.
 */
enum class AnalyticsFailureCode {
    NETWORK,
    TIMEOUT,
    AUTH,
    INSUFFICIENT_EVENT,
    SERVER,
    RESULT_UNAVAILABLE,
    UNKNOWN,
    ;

    companion object {
        fun from(error: Throwable): AnalyticsFailureCode =
            when (error) {
                is SocketTimeoutException -> TIMEOUT
                is ApiException.NetworkException -> NETWORK
                is ApiException.UnauthorizedException -> AUTH
                is ApiException.ServerException -> SERVER
                else -> UNKNOWN
            }

        /** 서버가 보고한 생성 실패 사유. 사용자가 바꿀 수 있는 입력 부족만 따로 가른다. */
        fun from(reason: DraftTaskFailureReason): AnalyticsFailureCode =
            when (reason) {
                DraftTaskFailureReason.STAGING_DATA_MISSING -> INSUFFICIENT_EVENT
                DraftTaskFailureReason.AI_REPORTED_FAILURE,
                DraftTaskFailureReason.AI_DISPATCH_FAILURE,
                DraftTaskFailureReason.FINALIZE_FAILURE,
                -> SERVER
                DraftTaskFailureReason.UNKNOWN -> UNKNOWN
            }
    }
}
