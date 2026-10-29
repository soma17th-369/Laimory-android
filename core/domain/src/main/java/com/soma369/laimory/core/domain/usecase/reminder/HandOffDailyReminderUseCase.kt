package com.soma369.laimory.core.domain.usecase.reminder

import com.soma369.laimory.core.domain.repository.DailyReminderRepository
import com.soma369.laimory.core.domain.repository.PushSettingsRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * 서버가 보내던 일일 리마인더를 끄고 기기 예약이 넘겨받게 한다.
 *
 * 서버 기능은 지우지 않는다. 구버전 앱은 여전히 서버 알림에 기대므로, 새 앱이 자기 계정의 서버 값만
 * 사용자 모르게 끈다. 서버 값이 켜져 있을 때마다 끄는 멱등 동작이라, 다른 계정으로 로그인하거나 새로
 * 가입한 계정(서버 기본값 켜짐)도 같은 경로를 탄다.
 *
 * 순서가 약속이다 — **끄기가 성공한 뒤에** 넘겨받았다고 적는다. 기기 예약은 이 표시를 보고서야 걸리므로,
 * 실패가 이어지는 동안에는 서버 알림이 하나만 오고, 넘겨받은 뒤에는 기기 알림이 하나만 온다.
 *
 * 기기에 켜짐 값이 아직 없으면 서버의 예전 선택을 처음 값으로 이어받는다. 꺼 둔 사람에게 다시 켜서
 * 보내지 않기 위해서다.
 *
 * 화면 밖에서 도는 일이라 공통 실패 안내(세션 만료 등)를 띄우지 않는다. 실패는 다음 기회에 다시 한다.
 */
class HandOffDailyReminderUseCase
    @Inject
    constructor(
        private val pushSettingsRepository: PushSettingsRepository,
        private val reminderRepository: DailyReminderRepository,
    ) {
        suspend operator fun invoke(): Result<Unit> =
            try {
                val server = pushSettingsRepository.getPushSettings()
                reminderRepository.setPushEnabledMirror(server.isPushEnabled)
                if (!reminderRepository.hasStoredEnabled()) {
                    reminderRepository.setEnabled(server.isDailyReminderEnabled)
                }
                if (server.isDailyReminderEnabled) {
                    pushSettingsRepository.updateDailyReminderEnabled(false)
                }
                reminderRepository.markServerReminderHandedOff()
                Result.success(Unit)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(exception)
            }
    }
