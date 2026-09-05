package com.soma369.laimory

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.soma369.laimory.collection.AutoCollectionProcessLifecycleObserver
import com.soma369.laimory.core.collection.health.sleep.detection.SleepDetectionEntryPoint
import com.soma369.laimory.core.util.logging.Logger
import com.soma369.laimory.draft.DraftTaskProcessLifecycleObserver
import com.soma369.laimory.push.DraftCompletionNotificationChannel
import com.soma369.laimory.push.PushRegistrationSessionObserver
import com.soma369.laimory.retention.SourceItemRetentionScheduler
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LaimoryApp :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var draftTaskProcessLifecycleObserver: DraftTaskProcessLifecycleObserver

    @Inject
    lateinit var autoCollectionProcessLifecycleObserver: AutoCollectionProcessLifecycleObserver

    @Inject
    lateinit var pushRegistrationSessionObserver: PushRegistrationSessionObserver

    @Inject
    lateinit var sourceItemRetentionScheduler: SourceItemRetentionScheduler

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun onCreate() {
        super.onCreate()
        applyLogLevel()
        ProcessLifecycleOwner.get().lifecycle.addObserver(draftTaskProcessLifecycleObserver)
        ProcessLifecycleOwner.get().lifecycle.addObserver(autoCollectionProcessLifecycleObserver)
        pushRegistrationSessionObserver.start()
        sourceItemRetentionScheduler.schedule()
        DraftCompletionNotificationChannel.create(this)
        // 수면 자동 감지 구독 복원. 사용자가 켜둔 상태였을 때만 다시 구독한다(시스템 구독이라 앱이 죽어도 유지).
        val subscriber =
            EntryPointAccessors
                .fromApplication(this, SleepDetectionEntryPoint::class.java)
                .sleepDetectionSubscriber()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { subscriber.startIfEnabled() }
    }

    /**
     * 앱 로그의 최소 레벨을 빌드 타입에 맞춘다.
     *
     * [Logger] 기본값은 `BuildConfig.DEBUG` 만 보므로 qa 가 release 와 같은 `WARN` 이 된다. QA 는
     * 로그로 들여다볼 수 있어야 하는 빌드라 여기서 다시 정한다 — 세 buildType 을 모두 아는 것은
     * 이 모듈뿐이다.
     *
     * release 는 `WARN` 을 유지한다. 앱 로그는 다른 앱이 읽을 수 없고, 민감정보 정책이 [Logger]
     * 에 이미 서 있어 필드에서 `adb` 로 확인할 수 있는 값이 더 크다.
     */
    private fun applyLogLevel() {
        Logger.minLevel =
            runCatching { Logger.Level.valueOf(BuildConfig.APP_LOG_LEVEL) }
                .getOrDefault(Logger.Level.WARN)
    }
}
