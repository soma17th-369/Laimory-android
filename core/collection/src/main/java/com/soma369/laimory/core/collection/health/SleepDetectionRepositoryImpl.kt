package com.soma369.laimory.core.collection.health

import com.soma369.laimory.core.domain.repository.SleepDetectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * [SleepDetectionRepository] 구현 — 자동 감지 "원함" 의도를 영속하고 구독을 즉시 반영한다(#142).
 *
 * 켜면 의도 저장 + [SleepDetectionSubscriber.start] 로 콜드 재실행 없이 바로 구독하고, 끄면 저장 + 구독 해제한다.
 * 실제 구독 성사 여부는 권한·Play Services 에 달렸고([SleepDetectionSubscriber] 가 판정), 여기선 의도만 관리한다.
 */
internal class SleepDetectionRepositoryImpl
    @Inject
    constructor(
        private val preferences: SleepDetectionPreferences,
        private val subscriber: SleepDetectionSubscriber,
    ) : SleepDetectionRepository {
        override fun observeEnabled(): Flow<Boolean> = preferences.observeEnabled()

        override suspend fun setEnabled(enabled: Boolean) {
            preferences.setEnabled(enabled)
            if (enabled) subscriber.start() else subscriber.stop()
        }
    }
