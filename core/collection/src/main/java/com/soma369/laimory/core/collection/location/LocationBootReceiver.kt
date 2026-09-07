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
 * 재부팅 후 위치 수집 복원.
 *
 * 켤지 말지는 여기서 판단하지 않고 [LocationTrackingRepository.reconcile] 에 맡긴다 — 전경 진입과
 * 같은 규칙을 써야 부팅에서만 켜지거나 부팅에서만 안 켜지는 어긋남이 생기지 않는다.
 *
 * 의존성은 @AndroidEntryPoint(BroadcastReceiver 의 super.onReceive 제약) 대신 [EntryPointAccessors] 로 직접 얻는다.
 */
internal class LocationBootReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface BootEntryPoint {
        fun locationTrackingRepository(): LocationTrackingRepository
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        val repository =
            EntryPointAccessors.fromApplication(appContext, BootEntryPoint::class.java).locationTrackingRepository()
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                repository.reconcile()
            } catch (e: Exception) {
                Logger.w(LogDomain.COLLECTION, "부팅 후 위치 수집 복원 실패: ${e.message}")
            } finally {
                pending.finish()
                scope.cancel()
            }
        }
    }
}
