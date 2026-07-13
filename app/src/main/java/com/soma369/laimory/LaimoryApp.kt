package com.soma369.laimory

import android.app.Application
import com.soma369.laimory.core.collection.health.SleepDetectionEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class LaimoryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 수면 자동 감지 구독 복원. 사용자가 켜둔 상태였을 때만 다시 구독한다(시스템 구독이라 앱이 죽어도 유지).
        val subscriber =
            EntryPointAccessors
                .fromApplication(this, SleepDetectionEntryPoint::class.java)
                .sleepDetectionSubscriber()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { subscriber.startIfEnabled() }
    }
}
