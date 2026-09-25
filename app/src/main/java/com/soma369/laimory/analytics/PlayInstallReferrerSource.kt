package com.soma369.laimory.analytics

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Play Install Referrer API 로 조회한다. Play 서비스를 아는 것은 이 클래스뿐이다.
 *
 * 연결은 조회 한 번마다 열고 닫는다 — 성공·실패·예외·취소 어느 경로든 `endConnection` 까지 간다.
 * 개발 APK 를 직접 설치하면 Play 를 거치지 않아 referrer 가 비어 온다.
 */
internal class PlayInstallReferrerSource(
    private val context: Context,
) : InstallReferrerSource {
    override suspend fun fetch(): InstallReferrerLookup {
        val client = InstallReferrerClient.newBuilder(context).build()
        return try {
            // 콜백이 끝내 오지 않으면 영영 기다리게 되므로 시간을 둔다.
            val responseCode =
                withTimeoutOrNull(CONNECT_TIMEOUT_MILLIS) { client.connect() }
                    ?: return InstallReferrerLookup.Transient
            when (responseCode) {
                InstallReferrerClient.InstallReferrerResponse.OK -> {
                    val details = withContext(Dispatchers.IO) { client.installReferrer }
                    InstallReferrerLookup.Found(
                        referrer = details.installReferrer,
                        clickAtSeconds = details.referrerClickTimestampSeconds,
                        installBeginAtSeconds = details.installBeginTimestampSeconds,
                    )
                }
                InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED,
                InstallReferrerClient.InstallReferrerResponse.DEVELOPER_ERROR,
                -> InstallReferrerLookup.Unsupported
                else -> InstallReferrerLookup.Transient
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // RemoteException·SecurityException 등. 연결 쪽 사정이라 다음 시도에서 풀릴 수 있다.
            InstallReferrerLookup.Transient
        } finally {
            runCatching { client.endConnection() }
        }
    }

    private suspend fun InstallReferrerClient.connect(): Int =
        suspendCancellableCoroutine { continuation ->
            startConnection(
                object : InstallReferrerStateListener {
                    override fun onInstallReferrerSetupFinished(responseCode: Int) {
                        if (continuation.isActive) continuation.resume(responseCode)
                    }

                    override fun onInstallReferrerServiceDisconnected() {
                        if (continuation.isActive) {
                            continuation.resume(InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED)
                        }
                    }
                },
            )
        }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 10_000L
    }
}
