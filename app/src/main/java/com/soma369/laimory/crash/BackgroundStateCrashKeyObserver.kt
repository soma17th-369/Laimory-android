package com.soma369.laimory.crash

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.usecase.ObserveLocationTrackingUseCase
import com.soma369.laimory.core.domain.usecase.sleep.ObserveSleepDetectionUseCase
import com.soma369.laimory.core.util.logging.Logger
import com.soma369.laimory.core.util.permission.LocationPermission
import com.soma369.laimory.core.util.permission.NotificationListenerAccess
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 배경에서 이어지는 상태를 크래시 리포트의 꼬리표로 유지한다 — 위치 수집, 수면 감지, 알림 접근, 초안 작업.
 *
 * 로그가 아니라 키로 두는 이유: 이 상태들은 **크래시보다 몇 시간 앞서** 시작된다(부팅 때 복원된 위치
 * 수집, 밤새 걸린 수면 구독, 분 단위 초안 폴링). 그때 남긴 로그는 크래시 시점에 이미 브레드크럼 창
 * 밖으로 밀려나 있다.
 *
 * 프로세스가 뜰 때마다 시작한다. 전경 없이 뜨는 프로세스(부팅 복원, 수면 이벤트 수신)에서 나는
 * 크래시가 바로 이 키가 필요한 경우다.
 *
 * 위치 권한과 알림 접근은 관찰할 흐름이 없는 시스템 설정이라 **전경 진입마다 다시 읽는다.** 설정
 * 화면에서 바꾸고 돌아오는 경로는 반드시 전경 진입을 지난다.
 */
@Singleton
class BackgroundStateCrashKeyObserver
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val observeLocationTracking: ObserveLocationTrackingUseCase,
        private val observeSleepDetection: ObserveSleepDetectionUseCase,
        private val draftTaskCoordinator: DraftTaskCoordinator,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    ) : DefaultLifecycleObserver {
        /** 시스템 설정을 다시 읽을 차례. 시작할 때 한 번 읽고, 이후 전경 진입마다 오른다. */
        private val settingsReads = MutableStateFlow(0)

        private var sessionJob: Job? = null

        fun start() {
            if (sessionJob?.isActive == true) return
            sessionJob =
                applicationScope.launch {
                    launch {
                        combine(observeLocationTracking(), settingsReads) { enabled, _ ->
                            locationTrackingKeyValue(enabled, LocationPermission.canTrack(context))
                        }.keep(CrashKey.LOCATION_TRACKING)
                    }
                    launch { observeSleepDetection().map(::sleepDetectionKeyValue).keep(CrashKey.SLEEP_DETECTION) }
                    launch {
                        settingsReads
                            .map { notificationAccessKeyValue(NotificationListenerAccess.isGranted(context)) }
                            .keep(CrashKey.NOTIFICATION_ACCESS)
                    }
                    // 처리 중에는 경과 시간이 바뀔 때마다 새 상태가 오므로, 단계가 바뀔 때만 쓴다.
                    launch { draftTaskCoordinator.state.map(::draftTaskKeyValue).keep(CrashKey.DRAFT_TASK) }
                }
        }

        override fun onStart(owner: LifecycleOwner) {
            settingsReads.update { it + 1 }
        }

        private suspend fun Flow<String>.keep(key: String) {
            distinctUntilChanged().collect { value -> Logger.setCrashKey(key, value) }
        }
    }
