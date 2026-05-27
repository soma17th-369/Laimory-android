package com.soma369.laimory.core.data.network.adapter

import com.soma369.laimory.core.data.dto.common.ApiResponse
import com.soma369.laimory.core.data.exception.ApiException
import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Retrofit의 [Call]을 감싸서 응답을 [Result]로 변환하는 래퍼.
 *
 * 서버 응답([ApiResponse])을 받아 아래 규칙으로 [Result]를 만든다.
 * - `success == true && data != null` → [Result.success]
 * - `success == false` → [Result.failure] ([ApiException.fromCode])
 * - HTTP 에러 코드 → [Result.failure] ([ApiException.fromCode])
 * - 네트워크 단절 등 [java.io.IOException] → [Result.failure] ([ApiException.NetworkException])
 * - 그 외 예외 → [Result.failure] ([ApiException.UnknownException])
 */
internal class ResultCall<T>(
    private val delegate: Call<ApiResponse<T>>,
) : Call<Result<T>> {
    override fun enqueue(callback: Callback<Result<T>>) {
        delegate.enqueue(
            object : Callback<ApiResponse<T>> {
                override fun onResponse(
                    call: Call<ApiResponse<T>>,
                    response: Response<ApiResponse<T>>,
                ) {
                    val body = response.body()
                    val result =
                        if (response.isSuccessful && body != null) {
                            if (body.success && body.data != null) {
                                Result.success(body.data)
                            } else {
                                val message = body.error?.message
                                Result.failure(ApiException.fromCode(response.code(), message))
                            }
                        } else {
                            Result.failure(ApiException.fromCode(response.code()))
                        }
                    callback.onResponse(this@ResultCall, Response.success(result))
                }

                override fun onFailure(
                    call: Call<ApiResponse<T>>,
                    t: Throwable,
                ) {
                    val exception =
                        if (t is java.io.IOException) {
                            ApiException.NetworkException()
                        } else {
                            ApiException.UnknownException(t.message)
                        }
                    callback.onResponse(this@ResultCall, Response.success(Result.failure(exception)))
                }
            },
        )
    }

    override fun clone(): Call<Result<T>> = ResultCall(delegate.clone())

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
 * @param responseType 실제 서버 응답 타입 ([ApiResponse]의 제네릭 T)
 */
internal class ResultCallAdapter<T>(
    private val responseType: Type,
) : CallAdapter<ApiResponse<T>, Call<Result<T>>> {
    override fun responseType(): Type = responseType

    override fun adapt(call: Call<ApiResponse<T>>): Call<Result<T>> = ResultCall(call)
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
 *     .addCallAdapterFactory(ResultCallAdapterFactory())
 *     ...
 * ```
 */
class ResultCallAdapterFactory : CallAdapter.Factory() {
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
        val responseType = ParameterizedTypeImpl(ApiResponse::class.java, resultType)

        return ResultCallAdapter<Any>(responseType)
    }
}

/**
 * 런타임에 제네릭 타입([ApiResponse]<T>)을 구성하기 위한 [ParameterizedType] 구현체.
 *
 * Java 리플렉션은 제네릭 타입 정보를 런타임에 직접 생성할 수 없으므로,
 * Retrofit이 올바른 역직렬화 타입을 인식할 수 있도록 직접 구현한다.
 */
private class ParameterizedTypeImpl(
    private val rawType: Class<*>,
    private val typeArgument: Type,
) : ParameterizedType {
    override fun getActualTypeArguments(): Array<Type> = arrayOf(typeArgument)

    override fun getRawType(): Type = rawType

    override fun getOwnerType(): Type? = null
}
