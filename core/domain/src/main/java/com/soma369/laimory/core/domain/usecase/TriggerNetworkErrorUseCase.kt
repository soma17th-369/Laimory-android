package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.helper.MessageHelper
import com.soma369.laimory.core.domain.repository.Feature1Repository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TriggerNetworkErrorUseCase
    @Inject
    constructor(
        private val repository: Feature1Repository,
        messageHelper: MessageHelper,
    ) : BaseUseCase(messageHelper) {
        suspend operator fun invoke(): Result<Unit> = execute { repository.triggerNetworkError() }
    }
