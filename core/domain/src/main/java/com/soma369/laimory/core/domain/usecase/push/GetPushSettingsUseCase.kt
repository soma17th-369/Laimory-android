package com.soma369.laimory.core.domain.usecase.push

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.model.push.PushSettings
import com.soma369.laimory.core.domain.repository.PushSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetPushSettingsUseCase
    @Inject
    constructor(
        private val repository: PushSettingsRepository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(): Result<PushSettings> = execute { repository.getPushSettings() }
    }
