package com.soma369.laimory.core.collection.di

import com.soma369.laimory.core.collection.collector.AndroidCollectionAvailabilityProvider
import com.soma369.laimory.core.collection.collector.CalendarCollector
import com.soma369.laimory.core.collection.collector.HealthCollector
import com.soma369.laimory.core.collection.collector.PhotoCollector
import com.soma369.laimory.core.domain.collector.Collector
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.provider.CollectionAvailabilityProvider
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

    @Binds
    @IntoMap
    @ItemTypeKey(ItemType.CALENDAR)
    abstract fun bindCalendarCollector(collector: CalendarCollector): Collector

    @Binds
    @IntoMap
    @ItemTypeKey(ItemType.HEALTH)
    abstract fun bindHealthCollector(collector: HealthCollector): Collector

    /** 자동 수집이 시도해도 되는 상태인지 판정하는 포트. 권한을 요청하지는 않는다. */
    @Binds
    abstract fun bindCollectionAvailabilityProvider(provider: AndroidCollectionAvailabilityProvider): CollectionAvailabilityProvider
}
