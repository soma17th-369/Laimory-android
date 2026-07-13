package com.soma369.laimory.core.collection.di

import android.content.Context
import com.soma369.laimory.core.collection.health.sleep.record.HealthConnectSleepGateway
import com.soma369.laimory.core.collection.health.sleep.record.SleepHealthGateway
import com.soma369.laimory.core.collection.health.sleep.record.SleepHealthRecorder
import com.soma369.laimory.core.collection.health.sleep.record.SleepRecordRepositoryImpl
import com.soma369.laimory.core.domain.repository.SleepRecordRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 수면 HC 기록(프로듀서) 배선.
 *
 * gateway(HC 구현) 바인딩 + 정책 recorder 제공. recorder 는 우리 앱 패키지명(정합성 판정 기준)이
 * 필요해 `context.packageName` 을 주입한다 — debug 는 `.debug` 접미사가 붙으므로 상수 대신 런타임 값을 쓴다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class SleepHealthModule {
    @Binds
    abstract fun bindSleepHealthGateway(impl: HealthConnectSleepGateway): SleepHealthGateway

    @Binds
    abstract fun bindSleepRecordRepository(impl: SleepRecordRepositoryImpl): SleepRecordRepository

    companion object {
        @Provides
        @Singleton
        fun provideSleepHealthRecorder(
            gateway: SleepHealthGateway,
            @ApplicationContext context: Context,
        ): SleepHealthRecorder = SleepHealthRecorder(gateway, context.packageName)
    }
}
