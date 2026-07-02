package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.base.BaseUseCase
import com.soma369.laimory.core.domain.notification.UserNotifier
import com.soma369.laimory.core.domain.repository.Feature1Repository

class TriggerNetworkErrorUseCase(
    private val repository: Feature1Repository,
    notifier: UserNotifier,
) : BaseUseCase(notifier) {
    suspend operator fun invoke(): Result<Unit> = execute { repository.triggerNetworkError() }
}
