package com.soma369.laimory.core.domain.exception

import java.io.IOException

/**
 * 서버 호출 실패.
 *
 * [message] 는 로그·디버깅용이다. 앱이 만든 개발용 문장("잘못된 … 형식입니다")이 들어갈 수 있어 사용자에게
 * 그대로 보이면 안 된다. 사용자에게 보일 문구는 [serverMessage] — 서버가 응답 봉투에 담아 준 안내 — 를 먼저 쓰고,
 * 없을 때만 앱 문구로 대신한다([serverMessageOr]).
 *
 * @property serverMessage 응답 봉투 `header.message`. 봉투가 없는 응답(ALB 오류 페이지, S3, 응답 해석 실패)이면 null.
 */
sealed class ApiException(
    override val message: String,
    val errorCode: Int? = null,
    val rawCode: Int? = null,
    val serverMessage: String? = null,
) : IOException(message) {
    class UnknownException(
        message: String? = null,
        errorCode: Int? = null,
        rawCode: Int? = null,
        serverMessage: String? = null,
    ) : ApiException(message ?: UNKNOWN_ERROR, errorCode, rawCode, serverMessage)

    class NetworkException : ApiException(NETWORK_ERROR)

    class UnauthorizedException(
        message: String? = null,
        errorCode: Int? = null,
        rawCode: Int? = null,
        serverMessage: String? = null,
    ) : ApiException(message ?: UNAUTHORIZED_ERROR, errorCode, rawCode, serverMessage)

    class ServerException(
        message: String? = null,
        errorCode: Int? = null,
        rawCode: Int? = null,
        serverMessage: String? = null,
    ) : ApiException(message ?: SERVER_ERROR, errorCode, rawCode, serverMessage)

    class ClientException(
        message: String? = null,
        errorCode: Int? = null,
        rawCode: Int? = null,
        serverMessage: String? = null,
    ) : ApiException(message ?: CLIENT_ERROR, errorCode, rawCode, serverMessage)

    class ConflictException(
        message: String? = null,
        errorCode: Int? = null,
        rawCode: Int? = null,
        serverMessage: String? = null,
    ) : ApiException(message ?: CONFLICT_ERROR, errorCode, rawCode, serverMessage)

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
            errorCode: Int? = null,
            serverMessage: String? = null,
        ): ApiException =
            when (code) {
                401, 403 -> UnauthorizedException(message, errorCode, code, serverMessage)
                409 -> ConflictException(message, errorCode, code, serverMessage)
                in 400..499 -> ClientException(message, errorCode, code, serverMessage)
                in 500..599 -> ServerException(message, errorCode, code, serverMessage)
                else -> UnknownException(message, errorCode, code, serverMessage)
            }
    }
}
