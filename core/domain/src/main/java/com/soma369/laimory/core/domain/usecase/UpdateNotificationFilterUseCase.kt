package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.NotificationFilter
import com.soma369.laimory.core.domain.repository.NotificationFilterRepository
import javax.inject.Inject
import javax.inject.Singleton

/** 알림 수집 필터 설정을 갱신한다. */
@Singleton
class UpdateNotificationFilterUseCase
    @Inject
    constructor(
        private val repository: NotificationFilterRepository,
    ) {
        suspend operator fun invoke(filter: NotificationFilter) = repository.update(filter)
    }
