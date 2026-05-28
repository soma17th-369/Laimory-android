package com.soma369.laimory.core.domain.exception

import java.io.IOException

sealed class ApiException(override val message: String) : IOException(message) {
    class UnknownException(message: String? = null) : ApiException(message ?: UNKNOWN_ERROR)

    class NetworkException : ApiException(NETWORK_ERROR)

    class UnauthorizedException(message: String? = null) : ApiException(message ?: UNAUTHORIZED_ERROR)

    class ServerException(message: String? = null) : ApiException(message ?: SERVER_ERROR)

    class ClientException(message: String? = null) : ApiException(message ?: CLIENT_ERROR)

    class ConflictException(message: String? = null) : ApiException(message ?: CONFLICT_ERROR)

    companion object {
        const val UNKNOWN_ERROR = "알 수 없는 에러 발생"
        const val NETWORK_ERROR = "네트워크 에러 발생"
        const val UNAUTHORIZED_ERROR = "인증이 필요합니다"
        const val SERVER_ERROR = "서버 에러 발생"
        const val CLIENT_ERROR = "잘못된 요청입니다"
        const val CONFLICT_ERROR = "중복된 요청입니다"

        fun fromCode(
            code: Int,
            message: String? = null,
        ): ApiException =
            when (code) {
                401, 403 -> UnauthorizedException(message)
                409 -> ConflictException(message)
                in 400..499 -> ClientException(message)
                in 500..599 -> ServerException(message)
                else -> UnknownException(message)
            }
    }
}
