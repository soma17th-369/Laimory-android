package com.soma369.laimory.core.domain.exception

import java.io.IOException

sealed class ApiException(
    override val message: String,
    val errorCode: String? = null,
    val rawCode: Int? = null,
) : IOException(message) {
    class UnknownException(
        message: String? = null,
        errorCode: String? = null,
        rawCode: Int? = null,
    ) : ApiException(message ?: UNKNOWN_ERROR, errorCode, rawCode)

    class NetworkException : ApiException(NETWORK_ERROR)

    class UnauthorizedException(
        message: String? = null,
        errorCode: String? = null,
        rawCode: Int? = null,
    ) : ApiException(message ?: UNAUTHORIZED_ERROR, errorCode = errorCode, rawCode = rawCode)

    class ServerException(
        message: String? = null,
        errorCode: String? = null,
        rawCode: Int? = null,
    ) : ApiException(message ?: SERVER_ERROR, errorCode = errorCode, rawCode = rawCode)

    class ClientException(
        message: String? = null,
        errorCode: String? = null,
        rawCode: Int? = null,
    ) : ApiException(message ?: CLIENT_ERROR, errorCode = errorCode, rawCode = rawCode)

    class ConflictException(
        message: String? = null,
        errorCode: String? = null,
        rawCode: Int? = null,
    ) : ApiException(message ?: CONFLICT_ERROR, errorCode = errorCode, rawCode = rawCode)

    companion object {
        const val UNKNOWN_ERROR = "알 수 없는 에러 발생"
        const val NETWORK_ERROR = "네트워크 에러 발생"
        const val UNAUTHORIZED_ERROR = "인증이 필요합니다"
        const val SERVER_ERROR = "서버 에러 발생"
        const val CLIENT_ERROR = "잘못된 요청입니다"
        const val CONFLICT_ERROR = "중복된 요청입니다"

        /**
         * HTTP 상태 코드로 [ApiException]을 만든다. 원본 코드는 [ApiException.rawCode]에 보존해
         * 상위 레이어가 401/404/5xx 등 코드 기준으로 정책을 분기할 수 있게 한다.
         */
        fun fromCode(
            code: Int,
            message: String? = null,
            errorCode: String? = null,
        ): ApiException =
            when (code) {
                401, 403 -> UnauthorizedException(message, errorCode = errorCode, rawCode = code)
                409 -> ConflictException(message, errorCode = errorCode, rawCode = code)
                in 400..499 -> ClientException(message, errorCode = errorCode, rawCode = code)
                in 500..599 -> ServerException(message, errorCode = errorCode, rawCode = code)
                else -> UnknownException(message, errorCode = errorCode, rawCode = code)
            }
    }
}
