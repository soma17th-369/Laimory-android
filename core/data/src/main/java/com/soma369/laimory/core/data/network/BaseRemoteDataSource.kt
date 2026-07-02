package com.soma369.laimory.core.data.network

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.domain.exception.ApiException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.Response
import java.io.IOException

/**
 * Retrofit 호출을 공통 [ApiException] 경로로 정규화하는 base DataSource.
 *
 * 모든 API 메서드는 `Response<ApiResponse<T>>`를 반환하고, RemoteDataSource는
 * [safeApiCall] / [safeApiCallForCompletion]로 감싸 성공 시 봉투의 data를 꺼내거나
 * 실패 시 [ApiException]으로 던진다. (이전 `ResultCallAdapterFactory`의 책임을 이관)
 *
 * 매핑 규칙:
 * - 네트워크 오류 ([IOException]) → [ApiException.NetworkException]
 * - HTTP 4xx/5xx → [ApiException.fromCode] (errorBody의 message 활용)
 * - HTTP 2xx + success=false → [ApiException.UnknownException] (error.detail / message 활용)
 * - HTTP 2xx + body/data null → [ApiException.UnknownException]
 * - 그 외 예외 → [ApiException.UnknownException]
 */
abstract class BaseRemoteDataSource(
    protected val json: Json,
) {
    /** 값이 있는 응답용. 성공 시 봉투의 data를 반환하고, 실패 시 [ApiException]을 던진다. */
    protected suspend fun <T> safeApiCall(call: suspend () -> Response<ApiResponse<T>>): T {
        val body = requireEnvelope(call)
        return body.data ?: throw ApiException.UnknownException()
    }

    /** 반환값이 없는(명령/트리거) 응답용. data를 요구하지 않고 성공/실패만 검증한다. */
    protected suspend fun safeApiCallForCompletion(call: suspend () -> Response<ApiResponse<Unit>>) {
        requireEnvelope(call)
    }

    private suspend fun <T> requireEnvelope(call: suspend () -> Response<ApiResponse<T>>): ApiResponse<T> {
        val response =
            try {
                call()
            } catch (e: IOException) {
                throw ApiException.NetworkException()
            } catch (e: Exception) {
                throw ApiException.UnknownException(e.message)
            }

        if (!response.isSuccessful) {
            throw ApiException.fromCode(response.code(), response.parseErrorMessage())
        }

        val body = response.body() ?: throw ApiException.UnknownException()
        if (!body.success) {
            // HTTP는 2xx지만 서버 비즈니스 로직 실패
            throw ApiException.UnknownException(body.error?.detail ?: body.message)
        }
        return body
    }

    private fun Response<*>.parseErrorMessage(): String? =
        try {
            errorBody()?.string()?.let { raw ->
                (json.parseToJsonElement(raw) as? JsonObject)
                    ?.get("message")
                    ?.jsonPrimitive
                    ?.contentOrNull
            }
        } catch (_: Exception) {
            null
        }
}
