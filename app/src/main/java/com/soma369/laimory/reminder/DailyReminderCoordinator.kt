package com.soma369.laimory.reminder

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.soma369.laimory.core.domain.di.ApplicationCoroutineScope
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.repository.DailyReminderRepository
import com.soma369.laimory.core.domain.usecase.auth.ObserveAuthSessionUseCase
import com.soma369.laimory.core.domain.usecase.reminder.HandOffDailyReminderUseCase
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 기기 리마인더 예약을 지금 조건에 맞춘다.
 *
 * 예약하는 조건은 넷이 모두 맞을 때다 — 로그인돼 있고, 서버 리마인더를 넘겨받았고(끄기 성공), 리마인더가 켜져
 * 있고, 전체 알림이 꺼져 있지 않다. 하나라도 바뀌면 그 자리에서 다시 걸거나 내린다. 로그아웃·탈퇴는 세션이
 * 바뀌는 것으로 잡힌다.
 *
 * 서버 넘겨받기는 로그인할 때마다 한다(다른 계정·새 계정도 서버 값이 켜져 있을 수 있다). 실패했으면 앱이
 * 전경으로 올라올 때 다시 한다.
 *
 * 넘겨받기 표시는 **지금 세션 계정의 것**이다. 로그인 직전에 비워지고(`NewSessionPreparer`), 앞 세션의 늦은 응답은
 * 세대가 달라 적히지 않는다. 남으면 다음 계정이 앞 계정의 표시로 예약을 허락받아, 자기 서버 끄기가 끝나기 전(또는
 * 실패해도) 서버 알림과 기기 알림이 함께 온다.
 *
 * 세션이 바뀌면 진행 중이던 넘겨받기는 취소한다([collectLatest]). 앞 세션 일이 잠금을 쥔 채 서버를 기다리면, 새
 * 세션의 넘겨받기가 그 뒤에 줄을 선다.
 */
@Singleton
class DailyReminderCoordinator
    @Inject
    constructor(
        private val repository: DailyReminderRepository,
        private val observeAuthSession: ObserveAuthSessionUseCase,
        private val handOffDailyReminder: HandOffDailyReminderUseCase,
        private val scheduler: DailyReminderAlarms,
        @ApplicationCoroutineScope private val applicationScope: CoroutineScope,
    ) : DefaultLifecycleObserver {
        private var isStarted = false

        /** 서버로 나가는 넘겨받기를 한 줄로 세운다. 시작 직후 세션 관찰과 전경 진입이 겹친다. */
        private val handOffLock = Mutex()

        /** 지금 걸어야 하는 시각. 걸지 않아야 하면 `null`. */
        private val activeTime: Flow<LocalTime?> =
            combine(
                observeAuthSession().filter { session -> session != AuthSessionState.Loading },
                repository.settings,
                repository.isServerReminderHandedOff,
                repository.pushEnabledMirror,
            ) { session, settings, isHandedOff, isPushEnabled ->
                settings.time.takeIf {
                    session == AuthSessionState.Authenticated &&
                        isHandedOff &&
                        settings.isEnabled &&
                        isPushEnabled != false
                }
            }.distinctUntilChanged()

        fun start() {
            if (isStarted) return
            isStarted = true
            applicationScope.launch { activeTime.collect(::apply) }
            applicationScope.launch {
                observeAuthSession()
                    .distinctUntilChanged()
                    .collectLatest { session ->
                        if (session == AuthSessionState.Authenticated) handOffLock.withLock { handOff() }
                    }
            }
        }

        override fun onStart(owner: LifecycleOwner) {
            applicationScope.launch { retryHandOffIfNeeded() }
        }

        /** 지금 계정의 넘겨받기가 아직 끝나지 않았으면 다시 한다. 앱이 전경으로 올라올 때 부른다. */
        suspend fun retryHandOffIfNeeded() {
            handOffLock.withLock {
                if (repository.isServerReminderHandedOff.first()) return@withLock
                if (observeAuthSession().first { it != AuthSessionState.Loading } != AuthSessionState.Authenticated) {
                    return@withLock
                }
                handOff()
            }
        }

        /** 지금 걸어야 하는 시각. 알람을 받은 쪽이 띄울지 다시 확인하는 데 쓴다. */
        suspend fun currentActiveTime(): LocalTime? = activeTime.first()

        /**
         * 지금 조건으로 다시 건다.
         *
         * 조건이 그대로여도 다시 걸어야 하는 때가 있다 — 알림을 띄운 뒤(다음 날로), 재부팅·업데이트로 알람이
         * 지워졌을 때, 기기 시각·시간대가 바뀌었을 때.
         */
        suspend fun resync() = apply(currentActiveTime())

        /** 수신기가 부른다. 끝나면 [onFinished] 로 알린다 — 수신기는 그때까지 프로세스를 붙잡는다. */
        fun resyncAsync(onFinished: () -> Unit) {
            applicationScope.launch {
                try {
                    resync()
                } catch (error: Exception) {
                    Logger.w(LogDomain.PUSH, "일일 리마인더 재예약 실패: ${error::class.simpleName}")
                } finally {
                    onFinished()
                }
            }
        }

        private suspend fun apply(time: LocalTime?) {
            if (time == null) {
                scheduler.cancel()
                Logger.i(LogDomain.PUSH, "일일 리마인더 예약 해제")
            } else {
                scheduler.schedule(time, repository.deliveryMemory().lastDeliveredRecordDate)
                Logger.i(LogDomain.PUSH, "일일 리마인더 예약($time)")
            }
        }

        private suspend fun handOff() {
            handOffDailyReminder()
                .onSuccess { Logger.i(LogDomain.PUSH, "서버 일일 리마인더 넘겨받기 완료") }
                .onFailure { error -> Logger.w(LogDomain.PUSH, "서버 일일 리마인더 넘겨받기 실패: ${error::class.simpleName}") }
        }
    }
