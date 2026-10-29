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
 * 서버를 기다리는 사이 세션이 바뀌면(로그아웃·다른 계정 로그인) 결과를 버린다 — 시작할 때 읽은 계정 세대가 그대로일
 * 때만 적는다. 앞 세션의 늦은 응답이 새 세션을 넘겨받은 것으로 만들면 서버 알림과 기기 알림이 함께 온다.
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
                var outcome = handOffOnce()
                var retries = 0
                // 기다리는 사이 세션이 바뀌었으면 지금 세션으로 다시 한다. 로그아웃·로그인이 빠르게 지나가면 인증
                // 상태가 그대로 "로그인"으로 보여, 이 호출 말고는 새 세션의 넘겨받기를 시작할 계기가 없다.
                while (outcome == Outcome.STALE && retries < MAX_STALE_RETRIES) {
                    outcome = handOffOnce()
                    retries++
                }
                if (outcome == Outcome.DONE) {
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("세션이 거듭 바뀌어 넘겨받지 못함"))
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(exception)
            }

        private suspend fun handOffOnce(): Outcome {
            val generation = reminderRepository.accountGeneration()
            val server = pushSettingsRepository.getPushSettings()
            reminderRepository.setPushEnabledMirror(server.isPushEnabled, generation)
            if (!reminderRepository.hasStoredEnabled()) {
                reminderRepository.setEnabled(server.isDailyReminderEnabled)
            }
            if (server.isDailyReminderEnabled) {
                pushSettingsRepository.updateDailyReminderEnabled(false)
            }
            return if (reminderRepository.markServerReminderHandedOff(generation)) Outcome.DONE else Outcome.STALE
        }

        private enum class Outcome {
            DONE,

            /** 기다리는 사이 세션이 바뀌어 결과를 버렸다. */
            STALE,
        }

        private companion object {
            const val MAX_STALE_RETRIES = 2
        }
    }
