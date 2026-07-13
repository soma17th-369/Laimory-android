package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.NotificationFilter
import com.soma369.laimory.core.domain.repository.NotificationFilterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** 알림 수집 필터 설정을 관찰한다(설정 UI·리스너 서비스 공유). */
@Singleton
class ObserveNotificationFilterUseCase
    @Inject
    constructor(
        private val repository: NotificationFilterRepository,
    ) {
        operator fun invoke(): Flow<NotificationFilter> = repository.observe()
    }
