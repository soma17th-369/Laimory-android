package com.soma369.laimory.core.collection.di

import com.soma369.laimory.core.collection.source.CalendarProviderSource
import com.soma369.laimory.core.collection.source.PhotoMediaSource
import com.soma369.laimory.core.domain.source.CalendarSource
import com.soma369.laimory.core.domain.source.PhotoSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 원본을 볼 때마다 읽는 조회 포트 바인딩. 전량 배치 수집기(`Collector`) 레지스트리와 분리된 경로다.
 * 사진은 범위 조회 뒤 고른 것만 수집하고, 일정은 저장하지 않고 조회만 한다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class CollectionSourceModule {
    @Binds
    abstract fun bindPhotoSource(impl: PhotoMediaSource): PhotoSource

    @Binds
    abstract fun bindCalendarSource(impl: CalendarProviderSource): CalendarSource
}
