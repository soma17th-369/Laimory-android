package com.soma369.laimory.core.data.network

import okhttp3.Request
import retrofit2.Invocation
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.HTTP
import retrofit2.http.OPTIONS
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import java.lang.reflect.Method

private const val UNKNOWN_PATH = "(경로 미상)"

/**
 * 로그에 적을 요청 이름. `메서드 경로템플릿`(`GET timeline/daily-records/{recordDate}`) 이다.
 *
 * **실제 URL 을 쓰지 않는다.** 경로에는 기록 날짜·작업·이벤트 식별자가 들어가고, 이 이름은 크래시
 * 리포트로 기기를 떠난다. Retrofit 이 요청마다 붙여 두는 [Invocation] 에서 API 선언의 템플릿을 읽는다.
 * Retrofit 이 만들지 않은 요청에는 템플릿이 없으므로 경로를 적지 않는다.
 */
internal fun Request.apiLogLabel(): String {
    val template = tag(Invocation::class.java)?.method()?.pathTemplate()
    return "$method ${template ?: UNKNOWN_PATH}"
}

private fun Method.pathTemplate(): String? =
    annotations
        .firstNotNullOfOrNull { annotation ->
            when (annotation) {
                is GET -> annotation.value
                is POST -> annotation.value
                is PUT -> annotation.value
                is PATCH -> annotation.value
                is DELETE -> annotation.value
                is HEAD -> annotation.value
                is OPTIONS -> annotation.value
                is HTTP -> annotation.path
                else -> null
            }
        }?.takeIf { it.isNotEmpty() }
