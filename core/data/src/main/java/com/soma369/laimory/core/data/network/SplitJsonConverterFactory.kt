package com.soma369.laimory.core.data.network

import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * 요청과 응답에 서로 다른 `Json` 을 쓴다.
 *
 * 두 방향이 `explicitNulls` 에 정반대를 원한다.
 *
 * - **응답**은 `false` 여야 한다. 서버가 nullable 필드를 빼고 보내도 `null` 로 받아 넘어간다.
 *   `true` 면 `MissingFieldException` 이라, 공통 래퍼의 `body` 하나만 빠져도 모든 API 가 죽는다.
 * - **요청**은 `true` 여야 한다. 서버가 `키는 필수, 값은 nullable` 로 요구하는 필드가 있는데,
 *   `false` 는 값이 null 인 키를 통째로 지워 400 이 된다.
 *
 * 전역을 한쪽으로 정하면 반대쪽에서 요청 본문을 손으로 조립해야 한다. 방향별로 나누면 양쪽 모두
 * 평범한 `@Serializable` DTO 로 표현된다.
 *
 * **키를 생략하려면 기본값을 준다.** `encodeDefaults` 가 꺼져 있어 기본값과 같은 프로퍼티는
 * 인코딩되지 않는다 — `val memo: String? = null` 은 여전히 키째 빠진다.
 */
internal class SplitJsonConverterFactory(
    private val send: Converter.Factory,
    private val receive: Converter.Factory,
) : Converter.Factory() {
    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<*, okhttp3.RequestBody>? = send.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<okhttp3.ResponseBody, *>? = receive.responseBodyConverter(type, annotations, retrofit)
}
