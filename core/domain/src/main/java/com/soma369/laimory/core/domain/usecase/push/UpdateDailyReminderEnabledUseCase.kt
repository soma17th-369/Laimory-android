package com.soma369.laimory.core.domain.usecase.push

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.repository.PushSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 일일 리마인더 수신만 켜고 끈다. 전체 푸시가 꺼져 있어도 이 값은 그대로 보존된다. */
@Singleton
class UpdateDailyReminderEnabledUseCase
    @Inject
    constructor(
        private val repository: PushSettingsRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(isEnabled: Boolean): Result<Unit> = execute { repository.updateDailyReminderEnabled(isEnabled) }
    }
