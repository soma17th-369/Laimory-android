package com.soma369.laimory.core.data.network.interceptor

import com.soma369.laimory.core.data.network.apiLogLabel
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * 모든 API 요청의 경로와 결과 코드를 남긴다. 호출부가 따로 적지 않아도 앱의 통신 전체가 덮인다.
 *
 * `safeApiCall` 이 아니라 여기에 두는 이유: 통신이 끊기거나 응답을 해석하지 못하면 `safeApiCall` 은
 * 예외만 받고 **요청을 볼 수 없다.** 인터셉터는 응답이 없어도 요청을 들고 있다. `safeApiCall` 을
 * 거치지 않는 토큰 갱신도 함께 덮인다.
 *
 * 경로는 실제 URL 이 아니라 선언의 템플릿이다([apiLogLabel]). 쿼리·헤더·본문도 넣지 않는다.
 * `HttpLoggingInterceptor` 와는 다른 축이다 — 그쪽은 개발용 Logcat 출력이라 release 에는 붙지 않고,
 * 이쪽은 크래시 리포트의 맥락이라 모든 빌드에 붙는다.
 *
 * 2xx 는 `INFO`, 그 밖의 코드와 통신 실패는 `WARN` 이다. 화면이 이미 다루는 실패라 non-fatal 로
 * 올리지 않는다. 호출자가 취소한 요청은 실패가 아니라 남기지 않는다 — 남기면 화면을 떠날 때마다
 * 줄이 쌓인다.
 *
 * 다른 인터셉터보다 먼저 붙인다. 목 응답과 토큰 갱신 재시도를 거친 최종 결과를 본다.
 */
internal class ApiCallLogInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response =
            try {
                chain.proceed(request)
            } catch (e: IOException) {
                if (!chain.call().isCanceled()) {
                    Logger.w(LogDomain.NETWORK, "${request.apiLogLabel()} → 통신 실패(${e::class.simpleName})")
                }
                throw e
            }
        val line = "${request.apiLogLabel()} → ${response.code}"
        if (response.isSuccessful) {
            Logger.i(LogDomain.NETWORK, line)
        } else {
            Logger.w(LogDomain.NETWORK, line)
        }
        return response
    }
}
