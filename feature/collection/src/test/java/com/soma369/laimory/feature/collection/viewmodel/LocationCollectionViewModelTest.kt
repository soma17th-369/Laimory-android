package com.soma369.laimory.feature.collection.viewmodel

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.LocationTrackingStatus
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.provider.LocationAddressResolver
import com.soma369.laimory.core.domain.repository.LocationTrackingRepository
import com.soma369.laimory.core.domain.repository.MovementAddressRepository
import com.soma369.laimory.core.domain.repository.SourceItemRepository
import com.soma369.laimory.core.domain.repository.StayAddressRepository
import com.soma369.laimory.core.domain.usecase.ClearCollectedLocationsUseCase
import com.soma369.laimory.core.domain.usecase.ObserveLocationTrackingStatusUseCase
import com.soma369.laimory.core.domain.usecase.ObserveLocationTrackingUseCase
import com.soma369.laimory.core.domain.usecase.ObserveSourceItemsUseCase
import com.soma369.laimory.core.domain.usecase.ResolveMovementAddressesUseCase
import com.soma369.laimory.core.domain.usecase.ResolveStayAddressUseCase
import com.soma369.laimory.core.domain.usecase.SetLocationTrackingUseCase
import com.soma369.laimory.feature.collection.state.LocationUiIntent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class LocationCollectionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `같은 rawId 주소 요청은 화면 생명주기에서 한 번만 실행한다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val resolver = RecordingLocationAddressResolver()
            val viewModel = createViewModel(resolver)
            runCurrent()
            val intent = LocationUiIntent.ResolveStayAddress("stay-1", 37.5, 126.9)

            viewModel.sendIntent(intent)
            viewModel.sendIntent(intent)
            runCurrent()

            assertEquals(1, resolver.resolveCount)
        }

    @Test
    fun `주소 해석이 실패해도 같은 화면 생명주기에서는 재요청하지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val resolver = RecordingLocationAddressResolver(failure = IllegalStateException("failed"))
            val viewModel = createViewModel(resolver)
            runCurrent()
            val intent = LocationUiIntent.ResolveStayAddress("stay-1", 37.5, 126.9)

            viewModel.sendIntent(intent)
            runCurrent()
            viewModel.sendIntent(intent)
            runCurrent()

            assertEquals(1, resolver.resolveCount)
        }

    private fun createViewModel(resolver: LocationAddressResolver): LocationCollectionViewModel {
        val sourceRepository = FakeSourceItemRepository()
        val trackingRepository = FakeLocationTrackingRepository()
        val addressRepository = FakeLocationAddressRepository()
        return LocationCollectionViewModel(
            observeSourceItemsUseCase = ObserveSourceItemsUseCase(sourceRepository),
            observeLocationTrackingUseCase = ObserveLocationTrackingUseCase(trackingRepository),
            observeLocationTrackingStatusUseCase = ObserveLocationTrackingStatusUseCase(trackingRepository),
            setLocationTrackingUseCase = SetLocationTrackingUseCase(trackingRepository),
            clearCollectedLocationsUseCase = ClearCollectedLocationsUseCase(sourceRepository),
            resolveMovementAddressesUseCase = ResolveMovementAddressesUseCase(resolver, addressRepository),
            resolveStayAddressUseCase = ResolveStayAddressUseCase(resolver, addressRepository),
        )
    }

    private class RecordingLocationAddressResolver(
        private val failure: Throwable? = null,
    ) : LocationAddressResolver {
        var resolveCount = 0

        override suspend fun resolve(
            latitude: Double,
            longitude: Double,
        ): String? {
            resolveCount++
            failure?.let { throw it }
            return "서울특별시 마포구"
        }
    }

    private class FakeLocationAddressRepository : StayAddressRepository, MovementAddressRepository {
        override suspend fun updateAddress(
            rawId: String,
            address: String,
        ): Boolean = true

        override suspend fun updateAddresses(
            rawId: String,
            startAddress: String?,
            endAddress: String?,
        ): Boolean = true
    }

    private class FakeSourceItemRepository : SourceItemRepository {
        private val items = MutableStateFlow(emptyList<SourceItem>())

        override suspend fun addAll(items: List<SourceItem>): Int = 0

        override suspend fun upsertAll(items: List<SourceItem>): Int = 0

        override fun observeAll(): Flow<List<SourceItem>> = items

        override suspend fun getLatestCollectedAt(itemType: ItemType): Instant? = null

        override suspend fun deleteExpired(cutoff: Instant): Int = 0

        override suspend fun clear(itemType: ItemType) = Unit
    }

    private class FakeLocationTrackingRepository : LocationTrackingRepository {
        private val enabled = MutableStateFlow(false)
        private val status = MutableStateFlow<LocationTrackingStatus?>(null)

        override fun observeEnabled(): Flow<Boolean> = enabled

        override fun observeStatus(): Flow<LocationTrackingStatus?> = status

        override suspend fun setEnabled(enabled: Boolean) {
            this.enabled.value = enabled
        }
    }
}
