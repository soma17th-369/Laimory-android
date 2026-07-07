package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.NotificationFilter
import com.soma369.laimory.core.domain.repository.NotificationFilterRepository

/** 알림 수집 필터 설정을 갱신한다. */
class UpdateNotificationFilterUseCase(
    private val repository: NotificationFilterRepository,
) {
    suspend operator fun invoke(filter: NotificationFilter) = repository.update(filter)
}
