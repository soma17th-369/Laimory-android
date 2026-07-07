package com.soma369.laimory.core.collection.di

import com.soma369.laimory.core.collection.collector.PhotoCollector
import com.soma369.laimory.core.domain.collector.Collector
import com.soma369.laimory.core.domain.model.collection.ItemType
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

/**
 * 카테고리별 [Collector] 를 [ItemType] 키로 멀티바인딩해 `Map<ItemType, Collector>` 레지스트리를 만든다.
 * 후속 #92~#95 수집기는 여기에 한 줄씩 추가한다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class CollectorModule {
    @Binds
    @IntoMap
    @ItemTypeKey(ItemType.PHOTO)
    abstract fun bindPhotoCollector(collector: PhotoCollector): Collector
}
