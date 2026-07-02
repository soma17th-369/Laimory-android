package com.soma369.laimory.core.data.network.interceptor

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

class MockInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val path = chain.request().url.encodedPath

        if (path.contains("feature1/error/network")) throw IOException("Mock network error")

        val (code, body) =
            when {
                path.contains("feature1/items") -> 200 to FEATURE1_ITEMS_RESPONSE
                path.contains("v1/intro") -> 200 to INTRO_RESPONSE
                path.contains("feature1/error/server") -> 500 to ""
                path.contains("feature1/error/unauthorized") -> 401 to ""
                else -> return chain.proceed(chain.request())
            }

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
            """{"header":{"code":"COMMON_0000","message":"success","transactionId":"mock-tx-items"},"body":[""" +
                """{"id":1,"title":"아이템 1","description":"첫 번째 아이템입니다"},""" +
                """{"id":2,"title":"아이템 2","description":"두 번째 아이템입니다"},""" +
                """{"id":3,"title":"아이템 3","description":"세 번째 아이템입니다"}""" +
                """]}"""

        private val INTRO_RESPONSE =
            """{"header":{"code":"COMMON_0000","message":"success","transactionId":"mock-tx-intro"},""" +
                """"body":{"minAppVersion":1,"recommendAppVersion":1,"debugTestMessage":null}}"""
    }
}
