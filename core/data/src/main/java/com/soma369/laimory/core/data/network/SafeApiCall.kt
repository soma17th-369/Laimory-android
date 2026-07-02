package com.soma369.laimory.core.data.network

import com.soma369.laimory.core.data.model.common.ApiResponse
import com.soma369.laimory.core.data.model.common.SUCCESS_CODE
import com.soma369.laimory.core.domain.exception.ApiException
import retrofit2.Response
import java.io.IOException

/**
 * Retrofit 호출을 공통 [ApiException] 경로로 정규화한다. (이전 ResultCallAdapterFactory·BaseRemoteDataSource 대체)
 *
 * 모든 API는 `Response<ApiResponse<T>>`를 반환하고, RemoteDataSource는 이 함수로 감싸
 * 성공 시 `body`를 반환하거나 실패 시 [ApiException]으로 던진다.
 *
 * 매핑 규칙:
 * - 네트워크 오류([IOException]) → [ApiException.NetworkException]
 * - HTTP 4xx/5xx → [ApiException.fromCode]
 * - HTTP 2xx + `header.code != SUCCESS_CODE` → header의 code/message로 [ApiException] (errorCode 보존)
 * - 성공 → `body` 반환
 *
 * 성공인데 `body`가 없는 무바디(204형) 응답은 현재 없으므로 다루지 않는다. 실제로 생기면 별도 처리.
 */
suspend fun <T> safeApiCall(call: suspend () -> Response<ApiResponse<T>>): T {
    val response =
        try {
            call()
        } catch (e: IOException) {
            throw ApiException.NetworkException()
        } catch (e: Exception) {
            throw ApiException.UnknownException(e.message)
        }

    if (!response.isSuccessful) {
        throw ApiException.fromCode(response.code())
    }

    val envelope = response.body() ?: throw ApiException.UnknownException()
    val header = envelope.header
    if (header.code != SUCCESS_CODE) {
        throw ApiException.UnknownException(header.message, errorCode = header.code)
    }
    return envelope.body ?: throw ApiException.UnknownException()
}
