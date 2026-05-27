package com.soma369.laimory.core.data.network.adapter

import com.soma369.laimory.core.data.exception.ApiException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
 * HTTP 상태 코드를 성공/실패의 기준으로 삼는다.
 * - HTTP 2xx + body 존재 → [Result.success]
 * - HTTP 2xx + body null → [Result.failure] ([ApiException.UnknownException])
 * - HTTP 4xx/5xx → errorBody를 파싱해 메시지 추출 후 [Result.failure] ([ApiException.fromCode])
 * - 네트워크 오류 ([IOException]) → [Result.failure] ([ApiException.NetworkException])
 * - 그 외 예외 → [Result.failure] ([ApiException.UnknownException])
 */
internal class ResultCall<T>(
    private val delegate: Call<T>,
    private val json: Json,
) : Call<Result<T>> {
    override fun enqueue(callback: Callback<Result<T>>) {
        delegate.enqueue(
            object : Callback<T> {
                override fun onResponse(
                    call: Call<T>,
                    response: Response<T>,
                ) {
                    callback.onResponse(this@ResultCall, Response.success(response.toResult()))
                }

                override fun onFailure(
                    call: Call<T>,
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

    private fun Response<T>.toResult(): Result<T> =
        if (isSuccessful) {
            body()?.let { Result.success(it) }
                ?: Result.failure(ApiException.UnknownException())
        } else {
            Result.failure(ApiException.fromCode(code(), parseErrorMessage()))
        }

    private fun Response<T>.parseErrorMessage(): String? =
        try {
            errorBody()?.string()?.let { json.decodeFromString<ErrorBody>(it).message }
        } catch (e: Exception) {
            null
        }

    override fun clone(): Call<Result<T>> = ResultCall(delegate.clone(), json)

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
 * Retrofit이 `Call<Result<T>>`를 반환하는 API 메서드를 만날 때 이 어댑터가 사용된다.
 *
 * @param responseType API 메서드의 실제 반환 타입 T
 * @param json 에러 바디 파싱에 사용할 [Json] 인스턴스
 */
internal class ResultCallAdapter<T>(
    private val responseType: Type,
    private val json: Json,
) : CallAdapter<T, Call<Result<T>>> {
    override fun responseType(): Type = responseType

    override fun adapt(call: Call<T>): Call<Result<T>> = ResultCall(call, json)
}

/**
 * [ResultCallAdapter]를 Retrofit에 등록하기 위한 팩토리.
 *
 * Retrofit 빌더에 [addCallAdapterFactory]로 추가하면,
 * 반환 타입이 `Call<Result<T>>`인 API 메서드에 자동으로 적용된다.
 *
 * 사용 예:
 * ```kotlin
 * Retrofit.Builder()
 *     .addCallAdapterFactory(ResultCallAdapterFactory(json))
 *     ...
 * ```
 *
 * @param json 에러 바디 파싱에 사용할 [Json] 인스턴스
 */
class ResultCallAdapterFactory(private val json: Json) : CallAdapter.Factory() {
    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): CallAdapter<*, *>? {
        // 반환 타입이 Call이 아니면 이 팩토리가 처리하지 않음
        if (getRawType(returnType) != Call::class.java) return null

        val callType = getParameterUpperBound(0, returnType as ParameterizedType)

        // Call의 타입 파라미터가 Result가 아니면 처리하지 않음
        if (getRawType(callType) != Result::class.java) return null

        val resultType = getParameterUpperBound(0, callType as ParameterizedType)

        return ResultCallAdapter<Any>(resultType, json)
    }
}

/**
 * 에러 바디에서 메시지만 추출하기 위한 최소 구조체.
 * 서버 에러 포맷의 최상위 [message] 필드만 파싱한다.
 */
@Serializable
private data class ErrorBody(
    val message: String? = null,
)
