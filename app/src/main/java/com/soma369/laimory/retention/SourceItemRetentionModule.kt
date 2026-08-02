package com.soma369.laimory.retention

import com.soma369.laimory.BuildConfig
import com.soma369.laimory.core.domain.model.collection.SourceItemRetentionConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.ZoneId
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SourceItemRetentionBindingModule {
    @Binds
    @Singleton
    abstract fun bindUniquePeriodicWorkEnqueuer(impl: WorkManagerUniquePeriodicWorkEnqueuer): UniquePeriodicWorkEnqueuer
}

@Module
@InstallIn(SingletonComponent::class)
internal object SourceItemRetentionRuntimeModule {
    @Provides
    @Singleton
    fun provideSourceItemRetentionConfig(): SourceItemRetentionConfig = SourceItemRetentionConfig(BuildConfig.SOURCE_ITEM_RETENTION_DAYS)

    /** 실행할 때마다 현재 기기 시간대를 다시 읽는다. */
    @Provides
    @Singleton
    fun provideSystemZoneId(): () -> ZoneId = { ZoneId.systemDefault() }
}
