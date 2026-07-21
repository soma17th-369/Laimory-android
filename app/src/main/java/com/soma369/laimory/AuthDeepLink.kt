package com.soma369.laimory

import com.soma369.laimory.core.domain.model.auth.SocialLoginCallback
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private const val CALLBACK_SCHEME = "https"
private const val CALLBACK_PATH = "/auth/app"

/** 앱이 소유한 OAuth callback origin/path만 도메인 결과로 변환한다. */
internal fun String.toSocialLoginCallbackOrNull(): SocialLoginCallback? {
    val uri = runCatching { URI(this) }.getOrNull() ?: return null
    if (!uri.scheme.equals(CALLBACK_SCHEME, ignoreCase = true)) return null
    if (!uri.host.equals(BuildConfig.AUTH_CALLBACK_HOST, ignoreCase = true)) return null
    if (uri.path != CALLBACK_PATH) return null

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
