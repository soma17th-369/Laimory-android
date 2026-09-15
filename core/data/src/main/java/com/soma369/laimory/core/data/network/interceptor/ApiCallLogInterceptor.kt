package com.soma369.laimory.core.data.network.interceptor

import com.soma369.laimory.core.data.network.apiLogLabel
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
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
 * **통신 실패는 두 시점에 난다.** 헤더를 받기 전의 실패는 [Interceptor.Chain.proceed] 가 던진다. 헤더를
 * 받은 뒤 본문을 읽다 끊기는 실패는 이 함수가 돌아간 뒤 Retrofit 컨버터가 본문을 읽을 때 나서 여기
 * catch 를 지나지 않는다. 그래서 본문을 [BodyReadFailureLogging] 으로 감싸 그 시점의 실패도 남긴다.
 *
 * 다른 인터셉터보다 먼저 붙인다. 목 응답과 토큰 갱신 재시도를 거친 최종 결과를 본다.
 */
internal class ApiCallLogInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val call = chain.call()
        val response =
            try {
                chain.proceed(request)
            } catch (e: IOException) {
                if (!call.isCanceled()) {
                    Logger.w(LogDomain.NETWORK, "${request.apiLogLabel()} → 통신 실패(${e::class.simpleName})")
                }
                throw e
            }
        val label = request.apiLogLabel()
        val line = "$label → ${response.code}"
        if (response.isSuccessful) {
            Logger.i(LogDomain.NETWORK, line)
        } else {
            Logger.w(LogDomain.NETWORK, line)
        }
        val body = response.body ?: return response
        return response.newBuilder().body(BodyReadFailureLogging(body, call, label)).build()
    }
}

/**
 * 본문을 읽다 난 통신 실패를 남기는 본문 래퍼.
 *
 * 헤더까지 받은 뒤 연결이 끊기거나 읽기 시간이 넘으면, 예외는 본문을 읽는 쪽(Retrofit 컨버터)에서 난다.
 * `safeApiCall` 은 그 예외를 받지만 요청을 볼 수 없어 경로를 모른다. 요청을 아는 여기서 남긴다.
 * 같은 본문에서 읽기가 거듭 실패해도 한 번만 남기고, 호출자가 취소해 끊긴 것은 남기지 않는다.
 */
private class BodyReadFailureLogging(
    private val delegate: ResponseBody,
    private val call: Call,
    private val label: String,
) : ResponseBody() {
    private var reported = false

    private val bufferedSource: BufferedSource by lazy {
        object : ForwardingSource(delegate.source()) {
            override fun read(
                sink: Buffer,
                byteCount: Long,
            ): Long =
                try {
                    super.read(sink, byteCount)
                } catch (e: IOException) {
                    report(e)
                    throw e
                }
        }.buffer()
    }

    override fun contentType(): MediaType? = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun source(): BufferedSource = bufferedSource

    override fun close() = delegate.close()

    private fun report(error: IOException) {
        if (reported || call.isCanceled()) return
        reported = true
        Logger.w(LogDomain.NETWORK, "$label → 본문 수신 실패(${error::class.simpleName})")
    }
}
