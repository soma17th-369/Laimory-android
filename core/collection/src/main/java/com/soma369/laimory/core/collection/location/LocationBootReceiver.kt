package com.soma369.laimory.core.collection.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
 * 재부팅 후 위치 수집 복원. 추적 원함([LocationTrackingPreferences]) 상태였으면 FGS 를 재시작한다.
 *
 * 의존성은 @AndroidEntryPoint(BroadcastReceiver 의 super.onReceive 제약) 대신 [EntryPointAccessors] 로 직접 얻는다.
 * 백그라운드 위치 권한이 없으면 서비스가 시작돼도 샘플링에 실패해 의도를 off 로 내린다([LocationCollectionService]).
 */
internal class LocationBootReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface BootEntryPoint {
        fun preferences(): LocationTrackingPreferences
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        val preferences = EntryPointAccessors.fromApplication(appContext, BootEntryPoint::class.java).preferences()
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                if (preferences.isEnabled()) {
                    appContext.startForegroundService(Intent(appContext, LocationCollectionService::class.java))
                }
            } catch (e: Exception) {
                Logger.w(LogDomain.COLLECTION, "부팅 후 위치 수집 복원 실패: ${e.message}")
            } finally {
                pending.finish()
                scope.cancel()
            }
        }
    }
}
