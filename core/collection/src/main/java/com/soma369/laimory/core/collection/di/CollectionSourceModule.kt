package com.soma369.laimory.core.collection.di

import com.soma369.laimory.core.collection.source.PhotoMediaSource
import com.soma369.laimory.core.domain.source.PhotoSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** 날짜 선택 수집 포트 바인딩. 전량 배치 수집기(`Collector`) 레지스트리와 분리된 사진 선택 수집 경로다. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class CollectionSourceModule {
    @Binds
    abstract fun bindPhotoSource(impl: PhotoMediaSource): PhotoSource
}
