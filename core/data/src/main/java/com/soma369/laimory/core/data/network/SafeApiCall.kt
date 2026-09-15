package com.soma369.laimory.core.data.network

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.common.SUCCESS_CODE
import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Retrofit 호출을 공통 [ApiException] 경로로 정규화한다. (이전 ResultCallAdapterFactory·BaseRemoteDataSource 대체)
 *
 * 모든 API는 `Response<ApiResponse<T>>`를 반환하고, RemoteDataSource는 이 함수로 감싸
 * 성공 시 `body`를 반환하거나 실패 시 [ApiException]으로 던진다.
 *
 * 매핑 규칙:
 * - 코루틴 취소([CancellationException]) → 그대로 전파 (구조적 동시성 유지)
 * - 네트워크 오류([IOException]) → [ApiException.NetworkException]
 * - HTTP 4xx/5xx → [ApiException.fromCode] (errorBody의 message/error를 예외 메시지로 전달)
 * - HTTP 2xx + `header.code != SUCCESS_CODE` → header의 code/message로 [ApiException] (errorCode 보존)
 * - 성공 → `body` 반환
 *
 * 성공인데 `body`가 없는 무바디(204형) 응답은 현재 없으므로 다루지 않는다. 실제로 생기면 별도 처리.
 *
 * 로그: 요청 경로와 HTTP 코드는 `ApiCallLogInterceptor` 가 남긴다. 여기서는 **봉투 수준의 실패**만
 * 남긴다 — 서버가 헤더에 담아 준 코드, 응답을 해석하지 못한 것. 서버가 준 메시지 문자열은 넣지 않는다.
 */
suspend fun <T> safeApiCall(call: suspend () -> Response<ApiResponse<T>>): T {
    val response = executeApiCall(call)
    return response.body()?.body ?: throw unknownResponse(response, "성공 응답에 body 없음")
}

/** 성공 body가 `null`인 로그아웃 같은 API를 공통 envelope 규칙으로 처리한다. */
suspend fun safeApiCallUnit(call: suspend () -> Response<ApiResponse<Unit>>) {
    executeApiCall(call)
}

private suspend fun <T> executeApiCall(call: suspend () -> Response<ApiResponse<T>>): Response<ApiResponse<T>> {
    val response =
        try {
            call()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            // 경로와 함께 인터셉터가 이미 남겼다. 헤더를 받기 전의 실패는 요청 단계에서, 본문을 읽다 끊긴
            // 실패는 인터셉터가 감싼 본문에서 남는다.
            throw ApiException.NetworkException()
        } catch (e: Exception) {
            // 역직렬화 예외에는 민감 응답 JSON이 포함될 수 있으므로 상위 레이어로 원문을 전달하지 않는다.
            // 응답을 받지 못하고 예외만 왔으므로 경로를 알 수 없다. 바로 앞의 인터셉터 줄이 경로를 말한다.
            Logger.w(LogDomain.NETWORK, "응답 처리 실패(${e::class.simpleName})")
            throw ApiException.UnknownException()
        }

    if (!response.isSuccessful) {
        val error = response.parseErrorEnvelope()
        error?.code?.let { code -> Logger.w(LogDomain.NETWORK, "${response.logLabel()} → 헤더 코드 $code") }
        throw ApiException.fromCode(response.code(), error?.message, error?.code)
    }

    val header = response.body()?.header ?: throw unknownResponse(response, "응답 봉투 없음")
    if (header.code != SUCCESS_CODE) {
        Logger.w(LogDomain.NETWORK, "${response.logLabel()} → 헤더 코드 ${header.code}")
        throw ApiException.UnknownException(header.message, errorCode = header.code)
    }
    return response
}

private fun Response<*>.logLabel(): String = raw().request.apiLogLabel()

/** 뜻을 알 수 없는 응답을 남기고 예외로 바꾼다. */
private fun unknownResponse(
    response: Response<*>,
    reason: String,
): ApiException.UnknownException {
    Logger.w(LogDomain.NETWORK, "${response.logLabel()} → $reason")
    return ApiException.UnknownException()
}

/** errorBody 파싱 전용 경량 Json (역직렬화 대상이 고정 필드라 컨버터와 분리). */
private val errorBodyJson = Json { ignoreUnknownKeys = true }

private data class ErrorEnvelope(
    val code: Int?,
    val message: String?,
)

/** HTTP 실패 응답에서 공통 envelope의 에러 코드·메시지를 보존하고 레거시 필드도 지원한다. */
private fun Response<*>.parseErrorEnvelope(): ErrorEnvelope? =
    try {
        errorBody()?.string()?.takeIf { it.isNotBlank() }?.let { raw ->
            val obj = errorBodyJson.parseToJsonElement(raw) as? JsonObject
            val header = obj?.get("header") as? JsonObject
            ErrorEnvelope(
                code = header?.get("code")?.jsonPrimitive?.intOrNull,
                message =
                    header?.get("message")?.jsonPrimitive?.contentOrNull
                        ?: obj?.get("message")?.jsonPrimitive?.contentOrNull
                        ?: obj?.get("error")?.jsonPrimitive?.contentOrNull,
            )
        }
    } catch (_: Exception) {
        null
    }
