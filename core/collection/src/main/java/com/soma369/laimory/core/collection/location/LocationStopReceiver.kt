package com.soma369.laimory.core.collection.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.soma369.laimory.core.domain.repository.LocationTrackingRepository
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 상시 알림의 `중지` 를 받는다. 설정 토글을 끈 것과 **같은 경로**로 흘린다.
 *
 * 알림이 서비스를 직접 세우지 않는 이유는, 끄겠다는 의사를 저장할 자리가 필요하기 때문이다.
 * 그 저장을 서비스가 하면 큐에서 늦게 처리된 중지가 그 사이에 사용자가 다시 켜 둔 값을 덮어쓴다.
 * 의사는 조작을 받은 곳이 그 자리에서 쓴다.
 *
 * 의존성은 @AndroidEntryPoint(BroadcastReceiver 의 super.onReceive 제약) 대신 [EntryPointAccessors] 로 직접 얻는다.
 */
internal class LocationStopReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface StopEntryPoint {
        fun locationTrackingRepository(): LocationTrackingRepository
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val appContext = context.applicationContext
        val repository =
            EntryPointAccessors.fromApplication(appContext, StopEntryPoint::class.java).locationTrackingRepository()
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                repository.setEnabled(false)
            } catch (e: Exception) {
                Logger.w(LogDomain.COLLECTION, "알림에서 위치 수집 중지 실패: ${e.message}")
            } finally {
                pending.finish()
                scope.cancel()
            }
        }
    }
}
