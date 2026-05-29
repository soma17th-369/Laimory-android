package com.soma369.laimory.core.data.network.interceptor

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class MockInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val path = chain.request().url.encodedPath
        if (!path.contains("feature1/items")) return chain.proceed(chain.request())

        val (code, body) = 200 to FEATURE1_ITEMS_RESPONSE
        return Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("OK")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    companion object {
        private val FEATURE1_ITEMS_RESPONSE =
            """{"success":true,"message":"ok","data":[""" +
                """{"id":1,"title":"아이템 1","description":"첫 번째 아이템입니다"},""" +
                """{"id":2,"title":"아이템 2","description":"두 번째 아이템입니다"},""" +
                """{"id":3,"title":"아이템 3","description":"세 번째 아이템입니다"}""" +
                """],"error":null}"""
    }
}
