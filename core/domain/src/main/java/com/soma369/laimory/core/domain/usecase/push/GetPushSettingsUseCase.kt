package com.soma369.laimory.core.domain.usecase.push

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.push.PushSettings
import com.soma369.laimory.core.domain.repository.DailyReminderRepository
import com.soma369.laimory.core.domain.repository.PushSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 서버의 푸시 수신 설정을 조회한다.
 *
 * 받은 전체 알림 값은 기기 리마인더가 띄울지 판정할 거울로 남긴다 — 알림을 띄우는 순간에는 서버에 물을 수 없다.
 */
@Singleton
class GetPushSettingsUseCase
    @Inject
    constructor(
        private val repository: PushSettingsRepository,
        private val reminderRepository: DailyReminderRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(): Result<PushSettings> =
            execute {
                repository.getPushSettings().also { settings ->
                    reminderRepository.setPushEnabledMirror(settings.isPushEnabled)
                }
            }
    }
