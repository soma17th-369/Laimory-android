package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.repository.Feature1Repository

class TriggerUnauthorizedErrorUseCase(
    private val repository: Feature1Repository,
) {
    suspend operator fun invoke() = repository.triggerUnauthorizedError()
}
