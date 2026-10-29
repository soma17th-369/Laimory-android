package com.soma369.laimory.reminder

import com.soma369.laimory.core.domain.exception.ApiException
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.auth.SignedInAccount
import com.soma369.laimory.core.domain.model.push.PushSettings
import com.soma369.laimory.core.domain.repository.AuthRepository
import com.soma369.laimory.core.domain.repository.PushSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import java.time.LocalDate
import java.time.LocalTime

class FakeAuthRepository(
    initial: AuthSessionState,
) : AuthRepository {
    val state = MutableStateFlow(initial)

    override fun observeSessionState(): Flow<AuthSessionState> = state

    override fun observeSignedInAccount(): Flow<SignedInAccount?> = MutableStateFlow(null)

    override suspend fun issueTokens(
        appCode: String,
        appVerifier: String,
    ) = Unit

    override suspend fun logout() = Unit

    override suspend fun clearSession() = Unit
}

/** 계정의 서버 푸시 설정. 로그인한 계정을 바꿔 끼우며 쓴다. */
class FakePushSettingsRepository : PushSettingsRepository {
    var settings = PushSettings(isPushEnabled = true, isDailyReminderEnabled = true)
    var updateFailure: ApiException? = null
    val dailyReminderUpdates = mutableListOf<Boolean>()

    override suspend fun getPushSettings(): PushSettings = settings

    override suspend fun updatePushEnabled(isEnabled: Boolean) = Unit

    override suspend fun updateDailyReminderEnabled(isEnabled: Boolean) {
        dailyReminderUpdates += isEnabled
        updateFailure?.let { throw it }
        settings = settings.copy(isDailyReminderEnabled = isEnabled)
    }
}

/** 마지막으로 건 시각. 내렸으면 `null`. */
class RecordingAlarms : DailyReminderAlarms {
    var scheduledTime: LocalTime? = null
        private set

    override fun schedule(
        time: LocalTime,
        deliveredRecordDate: LocalDate?,
    ) {
        scheduledTime = time
    }

    override fun cancel() {
        scheduledTime = null
    }
}

class RecordingNotifications : DailyReminderNotifications {
    val posted = mutableListOf<DailyReminderCopy>()

    override fun pickGeneral(lastIndex: Int?) = DailyReminderCopy.General(0)

    override fun pickCompleted(lastIndex: Int?) = DailyReminderCopy.Completed(0)

    override fun notify(
        copy: DailyReminderCopy,
        recordDate: LocalDate,
    ): Boolean {
        posted += copy
        return true
    }
}

/**
 * 앱 스코프 대역. 테스트 스케줄러를 함께 써서 `advanceUntilIdle` 이 이 안의 일도 돌린다.
 *
 * `backgroundScope` 는 쓰지 않는다 — 그 안의 일은 `advanceUntilIdle` 이 기다리지 않아 실행되지 않은 채 끝난다.
 * 끝없이 관찰하는 코루틴이 있어 [block] 이 끝나면 취소한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun TestScope.withApplicationScope(block: suspend TestScope.(CoroutineScope) -> Unit) {
    val applicationScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
    try {
        block(applicationScope)
    } finally {
        applicationScope.cancel()
    }
}
