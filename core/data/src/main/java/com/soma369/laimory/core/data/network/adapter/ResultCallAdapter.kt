package com.soma369.laimory.core.data.network.adapter

import com.soma369.laimory.core.data.dto.common.ApiResponse
import com.soma369.laimory.core.data.exception.ApiException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * 서버 응답을 [Result]로 변환하는 래퍼.
 *
 * Retrofit이 응답 바디를 [JsonElement]로 역직렬화한 뒤,
 * [ApiResponse] 래퍼 구조를 직접 파싱해 [dataSerializer]로 data 필드를 역직렬화한다.
 *
 * - HTTP 2xx + success=true → [Result.success]
 * - HTTP 2xx + success=false → [Result.failure] ([ApiException], message 필드 활용)
 * - HTTP 2xx + body null → [Result.failure] ([ApiException.UnknownException])
 * - HTTP 4xx/5xx → errorBody를 파싱해 메시지 추출 후 [Result.failure] ([ApiException.fromCode])
 * - 네트워크 오류 ([IOException]) → [Result.failure] ([ApiException.NetworkException])
 * - 그 외 예외 → [Result.failure] ([ApiException.UnknownException])
 */
internal class ResultCall<T>(
    private val delegate: Call<JsonElement>,
    private val json: Json,
    private val dataSerializer: KSerializer<T>,
) : Call<Result<T>> {
    override fun enqueue(callback: Callback<Result<T>>) {
        delegate.enqueue(
            object : Callback<JsonElement> {
                override fun onResponse(
                    call: Call<JsonElement>,
                    response: Response<JsonElement>,
                ) {
                    callback.onResponse(this@ResultCall, Response.success(response.toResult()))
                }

                override fun onFailure(
                    call: Call<JsonElement>,
                    t: Throwable,
                ) {
                    val exception =
                        if (t is IOException) {
                            ApiException.NetworkException()
                        } else {
                            ApiException.UnknownException(t.message)
                        }
                    callback.onResponse(this@ResultCall, Response.success(Result.failure(exception)))
                }
            },
        )
    }

    private fun Response<JsonElement>.toResult(): Result<T> {
        if (!isSuccessful) {
            return Result.failure(ApiException.fromCode(code(), parseErrorMessage()))
        }
        val jsonObject =
            body() as? JsonObject
                ?: return Result.failure(ApiException.UnknownException())
        val success =
            jsonObject["success"]?.jsonPrimitive?.booleanOrNull
                ?: return Result.failure(ApiException.UnknownException())
        val message = jsonObject["message"]?.jsonPrimitive?.contentOrNull

        return if (success) {
            val dataElement = jsonObject["data"] ?: JsonNull
            try {
                Result.success(json.decodeFromJsonElement(dataSerializer, dataElement))
            } catch (e: Exception) {
                Result.failure(ApiException.UnknownException(e.message))
            }
        } else {
            // HTTP 코드가 아닌 서버 비즈니스 로직 실패이므로 message를 그대로 사용
            Result.failure(ApiException.UnknownException(message))
        }
    }

    private fun Response<JsonElement>.parseErrorMessage(): String? =
        try {
            errorBody()?.string()?.let { body ->
                (json.parseToJsonElement(body) as? JsonObject)
                    ?.get("message")
                    ?.jsonPrimitive
                    ?.contentOrNull
            }
        } catch (_: Exception) {
            null
        }

    override fun clone(): Call<Result<T>> = ResultCall(delegate.clone(), json, dataSerializer)

    // 비동기 전용 어댑터이므로 동기 실행은 지원하지 않음
    override fun execute(): Response<Result<T>> = throw UnsupportedOperationException("ResultCall does not support synchronous execution")

    override fun isExecuted(): Boolean = delegate.isExecuted

    override fun cancel() = delegate.cancel()

    override fun isCanceled(): Boolean = delegate.isCanceled

    override fun request(): Request = delegate.request()

    override fun timeout(): Timeout = delegate.timeout()
}

/**
 * [ResultCall]을 생성하는 [CallAdapter].
 *
 * [responseType]은 [JsonElement]로 고정해 Retrofit이 바디를 raw JSON으로 수신하도록 하고,
 * [dataSerializer]로 data 필드를 T로 역직렬화한다.
 */
internal class ResultCallAdapter<T>(
    private val dataSerializer: KSerializer<T>,
    private val json: Json,
) : CallAdapter<JsonElement, Call<Result<T>>> {
    override fun responseType(): Type = JsonElement::class.java

    override fun adapt(call: Call<JsonElement>): Call<Result<T>> = ResultCall(call, json, dataSerializer)
}

/**
 * [ResultCallAdapter]를 Retrofit에 등록하기 위한 팩토리.
 *
 * 반환 타입이 `Call<Result<T>>`인 API 메서드에 자동으로 적용된다.
 * `serializer(resultType)`으로 T의 [KSerializer]를 런타임에 조회해 어댑터에 전달한다.
 *
 * 사용 예:
 * ```kotlin
 * Retrofit.Builder()
 *     .addCallAdapterFactory(ResultCallAdapterFactory(json))
 *     ...
 * ```
 */
class ResultCallAdapterFactory(private val json: Json) : CallAdapter.Factory() {
    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): CallAdapter<*, *>? {
        if (getRawType(returnType) != Call::class.java) return null

        val callType = getParameterUpperBound(0, returnType as ParameterizedType)

        if (getRawType(callType) != Result::class.java) return null

        val resultType = getParameterUpperBound(0, callType as ParameterizedType)

        val dataSerializer =
            try {
                serializer(resultType)
            } catch (_: Exception) {
                return null
            }

        return ResultCallAdapter(dataSerializer, json)
    }
}
