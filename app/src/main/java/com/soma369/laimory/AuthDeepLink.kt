package com.soma369.laimory

import com.soma369.laimory.core.domain.model.auth.SocialLoginCallback
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private const val CALLBACK_SCHEME = "https"

/** 서버 도메인 루트(`https://host/`)에서 로그인 콜백을 받을 host 를 꺼낸다. */
internal fun authCallbackHostOf(apiBaseUrl: String): String =
    requireNotNull(URI(apiBaseUrl).host?.takeIf(String::isNotBlank)) { "API base URL must contain a host: $apiBaseUrl" }

/** 서버가 로그인 결과를 돌려보내는 경로. App Link 와 Auth Tab 이 같은 경로를 기다린다. */
internal const val AUTH_CALLBACK_PATH = "/auth/app"

/**
 * 앱이 소유한 OAuth callback origin/path만 도메인 결과로 변환한다.
 *
 * [callbackHost] 는 이 프로세스가 붙은 서버의 host 다. 서버는 콜백을 **요청 host** 기준으로 돌려주므로
 * (`https://{host}/auth/app`), 로그인을 시작한 서버와 같은 host 만 받는다.
 */
internal fun String.toSocialLoginCallbackOrNull(callbackHost: String): SocialLoginCallback? {
    val uri = runCatching { URI(this) }.getOrNull() ?: return null
    if (!uri.scheme.equals(CALLBACK_SCHEME, ignoreCase = true)) return null
    if (!uri.host.equals(callbackHost, ignoreCase = true)) return null
    if (uri.path != AUTH_CALLBACK_PATH) return null

    val query = uri.rawQuery.toQueryMap()
    return SocialLoginCallback(
        appCode = query["code"],
        errorCode = query["error"],
    )
}

private fun String?.toQueryMap(): Map<String, String> {
    if (this.isNullOrBlank()) return emptyMap()
    return split('&')
        .mapNotNull { field ->
            val parts = field.split('=', limit = 2)
            val key = parts.firstOrNull()?.decodeQueryComponent()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val value = parts.getOrNull(1)?.decodeQueryComponent() ?: return@mapNotNull null
            key to value
        }.toMap()
}

private fun String.decodeQueryComponent(): String? =
    runCatching { URLDecoder.decode(this, StandardCharsets.UTF_8.name()) }
        .getOrNull()
