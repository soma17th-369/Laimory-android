package com.soma369.laimory.core.collection.health

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 재부팅 후 수면 감지 구독 복원. Sleep API 구독은 리부트로 사라지므로, 사용자가 켜둔 상태였으면 다시 건다.
 *
 * 의존성은 [EntryPointAccessors] 로 직접 얻는다(BroadcastReceiver 는 필드 주입 불가). 실제 재구독 여부는
 * [SleepDetectionSubscriber.startIfEnabled] 가 영속 의도·권한을 확인해 판정한다.
 */
internal class SleepBootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        val subscriber =
            EntryPointAccessors
                .fromApplication(appContext, SleepDetectionEntryPoint::class.java)
                .sleepDetectionSubscriber()
        val pending = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            runCatching { subscriber.startIfEnabled() }
                .onFailure { e -> Logger.w(LogDomain.COLLECTION, "부팅 후 수면 감지 복원 실패: ${e.message}") }
            pending.finish()
            scope.cancel()
        }
    }
}
