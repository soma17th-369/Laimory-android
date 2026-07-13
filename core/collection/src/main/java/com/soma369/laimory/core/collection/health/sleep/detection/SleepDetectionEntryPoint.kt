package com.soma369.laimory.core.collection.health.sleep.detection

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 앱 시작 시 수면 감지 구독을 걸기 위한 진입점.
 *
 * `Application` 은 Hilt 필드 주입이 안 되므로 `EntryPointAccessors` 로 [SleepDetectionSubscriber] 를 얻는다.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SleepDetectionEntryPoint {
    fun sleepDetectionSubscriber(): SleepDetectionSubscriber
}
