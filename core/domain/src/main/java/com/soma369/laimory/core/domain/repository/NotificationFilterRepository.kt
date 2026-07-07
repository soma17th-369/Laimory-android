package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.collection.NotificationFilter
import kotlinx.coroutines.flow.Flow

/**
 * 알림 수집 필터 설정의 로컬 저장 계약. 구현은 `:core:collection`(DataStore)이 소유한다.
 *
 * 알림 리스너 서비스와 설정 UI 가 같은 필터를 공유하도록, 관찰 가능한 단일 소스로 둔다.
 */
interface NotificationFilterRepository {
    /** 현재 필터 설정을 관찰한다. */
    fun observe(): Flow<NotificationFilter>

    /** 필터 설정을 갱신한다. */
    suspend fun update(filter: NotificationFilter)
}
