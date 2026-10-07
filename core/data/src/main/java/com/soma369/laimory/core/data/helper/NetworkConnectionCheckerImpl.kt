package com.soma369.laimory.core.data.helper

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.soma369.laimory.core.domain.helper.NetworkConnectionChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [NetworkConnectionChecker] 구현체. 기본 네트워크가 인터넷 용도로 잡혀 있는지만 본다.
 *
 * 검증 완료(`VALIDATED`)까지는 요구하지 않는다 — 검증이 막힌 망이나 검증 중인 순간에도 실제 요청은 나갈 수 있어,
 * 요구하면 멀쩡한 사용자를 막는다. 비행기 모드처럼 연결이 아예 없는 경우를 거르는 것이 목적이다.
 */
@Singleton
class NetworkConnectionCheckerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : NetworkConnectionChecker {
        override fun isConnected(): Boolean {
            val manager = context.getSystemService(ConnectivityManager::class.java) ?: return true
            val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }
