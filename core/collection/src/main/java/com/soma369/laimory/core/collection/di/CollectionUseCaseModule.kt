package com.soma369.laimory.core.collection.di

import com.soma369.laimory.core.domain.collector.Collector
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.repository.LocationTrackingRepository
import com.soma369.laimory.core.domain.repository.NotificationFilterRepository
import com.soma369.laimory.core.domain.repository.SourceItemRepository
import com.soma369.laimory.core.domain.source.PhotoSource
import com.soma369.laimory.core.domain.usecase.AddSourceItemsUseCase
import com.soma369.laimory.core.domain.usecase.ClearCollectedCalendarUseCase
import com.soma369.laimory.core.domain.usecase.ClearCollectedHealthUseCase
import com.soma369.laimory.core.domain.usecase.ClearCollectedLocationsUseCase
import com.soma369.laimory.core.domain.usecase.ClearCollectedNotificationsUseCase
import com.soma369.laimory.core.domain.usecase.ClearCollectedPhotosUseCase
import com.soma369.laimory.core.domain.usecase.CollectCalendarUseCase
import com.soma369.laimory.core.domain.usecase.CollectHealthUseCase
import com.soma369.laimory.core.domain.usecase.CollectPhotosUseCase
import com.soma369.laimory.core.domain.usecase.CollectSelectedPhotosUseCase
import com.soma369.laimory.core.domain.usecase.GetPhotosOnDateUseCase
import com.soma369.laimory.core.domain.usecase.ObserveLocationTrackingStatusUseCase
import com.soma369.laimory.core.domain.usecase.ObserveLocationTrackingUseCase
import com.soma369.laimory.core.domain.usecase.ObserveNotificationFilterUseCase
import com.soma369.laimory.core.domain.usecase.ObserveSourceItemsUseCase
import com.soma369.laimory.core.domain.usecase.SetLocationTrackingUseCase
import com.soma369.laimory.core.domain.usecase.UpdateNotificationFilterUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 수집 영역 UseCase 조립. 수집 저장 계약의 구현을 이 모듈이 소유하므로
 * (`:core:data`의 remote UseCase 조립과 분리) UseCase 제공도 여기서 담당한다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object CollectionUseCaseModule {
    @Provides
    @Singleton
    fun provideObserveSourceItemsUseCase(repository: SourceItemRepository): ObserveSourceItemsUseCase =
        ObserveSourceItemsUseCase(repository)

    @Provides
    @Singleton
    fun provideAddSourceItemsUseCase(repository: SourceItemRepository): AddSourceItemsUseCase = AddSourceItemsUseCase(repository)

    @Provides
    @Singleton
    fun provideCollectPhotosUseCase(
        collectors: Map<ItemType, @JvmSuppressWildcards Collector>,
        repository: SourceItemRepository,
    ): CollectPhotosUseCase = CollectPhotosUseCase(collectors, repository)

    @Provides
    @Singleton
    fun provideGetPhotosOnDateUseCase(photoSource: PhotoSource): GetPhotosOnDateUseCase = GetPhotosOnDateUseCase(photoSource)

    @Provides
    @Singleton
    fun provideCollectSelectedPhotosUseCase(
        photoSource: PhotoSource,
        repository: SourceItemRepository,
    ): CollectSelectedPhotosUseCase = CollectSelectedPhotosUseCase(photoSource, repository)

    @Provides
    @Singleton
    fun provideClearCollectedPhotosUseCase(repository: SourceItemRepository): ClearCollectedPhotosUseCase =
        ClearCollectedPhotosUseCase(repository)

    @Provides
    @Singleton
    fun provideCollectCalendarUseCase(
        collectors: Map<ItemType, @JvmSuppressWildcards Collector>,
        repository: SourceItemRepository,
    ): CollectCalendarUseCase = CollectCalendarUseCase(collectors, repository)

    @Provides
    @Singleton
    fun provideClearCollectedCalendarUseCase(repository: SourceItemRepository): ClearCollectedCalendarUseCase =
        ClearCollectedCalendarUseCase(repository)

    @Provides
    @Singleton
    fun provideCollectHealthUseCase(
        collectors: Map<ItemType, @JvmSuppressWildcards Collector>,
        repository: SourceItemRepository,
    ): CollectHealthUseCase = CollectHealthUseCase(collectors, repository)

    @Provides
    @Singleton
    fun provideClearCollectedHealthUseCase(repository: SourceItemRepository): ClearCollectedHealthUseCase =
        ClearCollectedHealthUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveNotificationFilterUseCase(repository: NotificationFilterRepository): ObserveNotificationFilterUseCase =
        ObserveNotificationFilterUseCase(repository)

    @Provides
    @Singleton
    fun provideUpdateNotificationFilterUseCase(repository: NotificationFilterRepository): UpdateNotificationFilterUseCase =
        UpdateNotificationFilterUseCase(repository)

    @Provides
    @Singleton
    fun provideClearCollectedNotificationsUseCase(repository: SourceItemRepository): ClearCollectedNotificationsUseCase =
        ClearCollectedNotificationsUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveLocationTrackingUseCase(repository: LocationTrackingRepository): ObserveLocationTrackingUseCase =
        ObserveLocationTrackingUseCase(repository)

    @Provides
    @Singleton
    fun provideSetLocationTrackingUseCase(repository: LocationTrackingRepository): SetLocationTrackingUseCase =
        SetLocationTrackingUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveLocationTrackingStatusUseCase(repository: LocationTrackingRepository): ObserveLocationTrackingStatusUseCase =
        ObserveLocationTrackingStatusUseCase(repository)

    @Provides
    @Singleton
    fun provideClearCollectedLocationsUseCase(repository: SourceItemRepository): ClearCollectedLocationsUseCase =
        ClearCollectedLocationsUseCase(repository)
}
