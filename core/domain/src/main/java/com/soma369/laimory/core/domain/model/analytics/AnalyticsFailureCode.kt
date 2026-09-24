package com.soma369.laimory.core.domain.model.analytics

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.exception.HandledException
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

    /**
     * 사용자가 고칠 수 있는 입력 한도 위반으로 서버가 거절했다 — 사진 크기·개수·형식.
     *
     * 스펙에 없던 값이다. 이 거절은 4xx 라 다른 칸에 맞지 않아 `UNKNOWN` 으로 떨어졌는데, 원인이 앱 화면
     * (사진 고르기)에 있어 따로 보여야 고칠 곳을 안다.
     */
    INVALID_INPUT,
    SERVER,
    RESULT_UNAVAILABLE,
    UNKNOWN,
    ;

    companion object {
        /** 서버가 입력 한도 위반으로 정해 둔 코드. 사진 개수(-1004) · 크기(-1005) · 형식(-1007). */
        private val INPUT_LIMIT_ERROR_CODES = setOf(-1004, -1005, -1007)

        fun from(error: Throwable): AnalyticsFailureCode =
            when (error) {
                // 공통 안내를 띄운 실패는 원본을 감싸 온다. 열어 보지 않으면 서버 오류까지 UNKNOWN 이 된다.
                is HandledException -> from(error.cause)
                is SocketTimeoutException -> TIMEOUT
                is ApiException.NetworkException -> NETWORK
                is ApiException.UnauthorizedException -> AUTH
                is ApiException.ServerException -> SERVER
                // 다른 4xx 는 원인을 단정할 수 없어 UNKNOWN 으로 둔다.
                is ApiException.ClientException -> if (error.errorCode in INPUT_LIMIT_ERROR_CODES) INVALID_INPUT else UNKNOWN
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
