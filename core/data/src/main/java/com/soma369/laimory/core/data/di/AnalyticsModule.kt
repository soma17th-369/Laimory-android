package com.soma369.laimory.core.data.di

import com.soma369.laimory.core.data.analytics.AnalyticsDedupeStore
import com.soma369.laimory.core.data.analytics.PreferencesAnalyticsDedupeStore
import com.soma369.laimory.core.data.analytics.PreferencesAnalyticsTimelineEditLogRepository
import com.soma369.laimory.core.data.helper.AnalyticsHelperImpl
import com.soma369.laimory.core.domain.helper.AnalyticsHelper
import com.soma369.laimory.core.domain.repository.AnalyticsTimelineEditLogRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 제품 분석 배선.
 *
 * 공용 [HelperModule] 이 아니라 여기 두는 이유는 구현과 판정 저장소를 모듈 밖으로 드러내지 않기
 * 위해서다. 밖에서 쓰는 것은 도메인 포트([AnalyticsHelper])와 버킷 포트뿐이다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class AnalyticsModule {
    @Binds
    @Singleton
    abstract fun bindAnalyticsHelper(impl: AnalyticsHelperImpl): AnalyticsHelper

    @Binds
    @Singleton
    abstract fun bindAnalyticsDedupeStore(impl: PreferencesAnalyticsDedupeStore): AnalyticsDedupeStore

    @Binds
    @Singleton
    abstract fun bindAnalyticsTimelineEditLogRepository(
        impl: PreferencesAnalyticsTimelineEditLogRepository,
    ): AnalyticsTimelineEditLogRepository
}
