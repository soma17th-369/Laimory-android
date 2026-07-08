package com.soma369.laimory.feature.timeline.testexport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 임시 테스트 전용(삭제 예정) — 수집 계정 refresh_token 으로 Drive 액세스 토큰을 발급한다.
 *
 * `testexport` 패키지 전체는 사람 확인용 Drive 내보내기(#121)를 위한 개발/검증 도구다.
 * 나중에 이 패키지를 통째로 지우면 되도록 core/다른 feature 를 건드리지 않고 자체 완결로 둔다.
 *
 * 서비스 계정은 무료 계정 저장 용량이 0이라 못 올리므로, 저장 용량 있는 실제 계정의
 * refresh_token 을 심어두고 액세스 토큰을 자동 갱신한다. 스코프는 `drive.file` 권장.
 */
internal object DriveOAuth {
    private const val TOKEN_URI = "https://oauth2.googleapis.com/token"

    private var cachedKey: String? = null
    private var cachedToken: String? = null
    private var cachedExpiryMs: Long = 0

    suspend fun getAccessToken(
        clientId: String,
        clientSecret: String,
        refreshToken: String,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val now = System.currentTimeMillis()
                val key = "$clientId|$refreshToken"
                cachedToken?.let { if (key == cachedKey && now < cachedExpiryMs - 60_000) return@runCatching it }

                val body =
                    "grant_type=refresh_token" +
                        "&client_id=${URLEncoder.encode(clientId, "UTF-8")}" +
                        "&client_secret=${URLEncoder.encode(clientSecret, "UTF-8")}" +
                        "&refresh_token=${URLEncoder.encode(refreshToken, "UTF-8")}"
                val (code, text) = postForm(TOKEN_URI, body)
                if (code !in 200..299) error("토큰 갱신 실패 ($code): ${text.take(300)}")

                val res = JSONObject(text)
                val token = res.getString("access_token")
                cachedKey = key
                cachedToken = token
                cachedExpiryMs = now + res.optLong("expires_in", 3600) * 1000
                token
            }
        }

    /** client_id/client_secret/refresh_token 이 담긴 JSON(drive_oauth.json 형식)을 파싱해 토큰 발급. */
    suspend fun getAccessTokenFromJson(configJson: String): Result<String> {
        val cfg =
            runCatching { JSONObject(configJson) }.getOrElse {
                return Result.failure(IllegalArgumentException("JSON 형식 오류: ${it.message}"))
            }
        val clientId = cfg.optString("client_id")
        val clientSecret = cfg.optString("client_secret")
        val refreshToken = cfg.optString("refresh_token")
        if (clientId.isBlank() || clientSecret.isBlank() || refreshToken.isBlank()) {
            return Result.failure(IllegalArgumentException("client_id / client_secret / refresh_token 이 필요합니다"))
        }
        return getAccessToken(clientId, clientSecret, refreshToken)
    }

    private fun postForm(
        urlStr: String,
        body: String,
    ): Pair<Int, String> {
        val conn =
            (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 30_000
            }
        try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            return code to (stream?.bufferedReader()?.use { it.readText() } ?: "")
        } finally {
            conn.disconnect()
        }
    }
}
