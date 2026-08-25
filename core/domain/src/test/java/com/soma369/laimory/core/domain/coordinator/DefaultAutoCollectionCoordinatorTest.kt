package com.soma369.laimory.core.domain.coordinator

import com.soma369.laimory.core.domain.collector.Collector
import com.soma369.laimory.core.domain.model.collection.AutoCollectionOutcome
import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.provider.CollectionAvailabilityProvider
import com.soma369.laimory.core.domain.repository.SourceItemRepository
import com.soma369.laimory.core.domain.usecase.CollectCalendarUseCase
import com.soma369.laimory.core.domain.usecase.CollectHealthUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAutoCollectionCoordinatorTest {
    @Test
    fun `권한이 있으면 두 유형을 수집한다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = RecordingRepository()
            val coordinator = coordinator(repository = repository)

            val result = coordinator.refresh()

            assertEquals(AutoCollectionOutcome.Collected(1), result.outcomes[ItemType.CALENDAR])
            assertEquals(AutoCollectionOutcome.Collected(1), result.outcomes[ItemType.HEALTH])
            assertFalse(result.isIncomplete)
        }

    @Test
    fun `권한이 없으면 수집기를 부르지 않고 건너뛴다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 수집기 계약이 권한 없음도 빈 목록으로 흡수해서, 호출 경계가 따로 판정해야 구분된다.
            val repository = RecordingRepository()
            val coordinator = coordinator(availability = FakeAvailability(calendar = false), repository = repository)

            val result = coordinator.refresh()

            assertEquals(AutoCollectionOutcome.PermissionDenied, result.outcomes[ItemType.CALENDAR])
            assertEquals(0, repository.addAllCalls)
        }

    @Test
    fun `Health Connect 를 못 쓰면 권한 없음과 다른 결과로 구분한다`() =
        runTest(UnconfinedTestDispatcher()) {
            val coordinator = coordinator(availability = FakeAvailability(healthAvailable = false))

            val result = coordinator.refresh()

            assertEquals(AutoCollectionOutcome.Unavailable, result.outcomes[ItemType.HEALTH])
        }

    @Test
    fun `한 유형이 실패해도 다른 유형은 수집한다`() =
        runTest(UnconfinedTestDispatcher()) {
            val coordinator = coordinator(repository = RecordingRepository(failCalendar = true))

            val result = coordinator.refresh()

            assertEquals(AutoCollectionOutcome.Failed, result.outcomes[ItemType.CALENDAR])
            assertEquals(AutoCollectionOutcome.Collected(1), result.outcomes[ItemType.HEALTH])
            assertTrue(result.isIncomplete)
        }

    @Test
    fun `최근에 끝난 유형은 다시 수집하지 않는다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = RecordingRepository()
            val coordinator = coordinator(repository = repository)
            coordinator.refresh()

            val second = coordinator.refresh()

            assertTrue(second.outcomes.isEmpty())
            assertFalse(second.isIncomplete)
            assertEquals(1, repository.addAllCalls)
        }

    @Test
    fun `실패한 유형만 곧바로 다시 시도한다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 완료 시각을 하나로 두면 성공한 유형까지 5분마다 다시 긁거나, 실패한 유형이 5분간 묶인다.
            val repository = RecordingRepository(failCalendar = true)
            val coordinator = coordinator(repository = repository)
            coordinator.refresh()

            val second = coordinator.refresh()

            assertEquals(setOf(ItemType.CALENDAR), second.outcomes.keys)
            assertEquals(2, repository.addAllAttempts)
        }

    @Test
    fun `권한 없음은 캐시하지 않아 권한을 켜면 곧바로 수집한다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 권한은 사용자가 설정에서 바꾸는 값이라 "재시도해도 같은 답" 이 아니다.
            // 캐시하면 권한을 켜고 돌아와도 창이 닫힐 때까지 수집이 건너뛰어진다.
            val availability = MutableAvailability(calendar = false)
            val repository = RecordingRepository()
            val coordinator = coordinator(availability = availability, repository = repository)
            coordinator.refresh()
            assertEquals(0, repository.addAllCalls)

            availability.calendar = true
            val second = coordinator.refresh()

            assertEquals(AutoCollectionOutcome.Collected(1), second.outcomes[ItemType.CALENDAR])
            assertEquals(1, repository.addAllCalls)
        }

    @Test
    fun `Health Connect 미지원도 캐시하지 않는다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 앱을 켜 둔 채 Health Connect 를 설치·업데이트할 수 있다.
            val availability = MutableAvailability(healthAvailable = false)
            val coordinator = coordinator(availability = availability)
            coordinator.refresh()

            availability.healthAvailable = true
            val second = coordinator.refresh()

            assertEquals(AutoCollectionOutcome.Collected(1), second.outcomes[ItemType.HEALTH])
        }

    @Test
    fun `세션이 바뀌면 그 전에 시작한 작업의 결과를 최신성에 반영하지 않는다`() =
        runTest(UnconfinedTestDispatcher()) {
            // discard 가 코루틴만 예약하고 반환하면, 새 세션의 refresh 가 이전 결과를 물려받는다.
            val repository = RecordingRepository(gate = CompletableDeferred())
            val coordinator = coordinator(repository = repository)
            val first = async { coordinator.refresh() }
            runCurrent()

            coordinator.discard()
            repository.gate?.complete(Unit)
            // 진행 중이던 작업은 세션과 함께 취소된다 — 그 결과를 기다리는 쪽은 실패로 끝난다.
            runCatching { first.await() }

            // 이전 세션 결과가 캐시됐다면 여기서 건너뛴다.
            val second = coordinator.refresh()
            assertEquals(AutoCollectionOutcome.Collected(1), second.outcomes[ItemType.CALENDAR])
        }

    @Test
    fun `동시에 불러도 수집은 한 번만 돈다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = RecordingRepository(gate = CompletableDeferred())
            val coordinator = coordinator(repository = repository)

            val first = async { coordinator.refresh() }
            val second = async { coordinator.refresh() }
            runCurrent()
            repository.gate?.complete(Unit)

            first.await()
            second.await()
            assertEquals(1, repository.addAllCalls)
        }

    @Test
    fun `상한을 넘기면 기다림을 포기하고 최신 확보 실패로 알린다`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = RecordingRepository(gate = CompletableDeferred())
            val coordinator = coordinator(repository = repository)

            val result = coordinator.refresh(timeoutMillis = 1)

            assertTrue(result.timedOut)
            assertTrue(result.isIncomplete)
            repository.gate?.complete(Unit)
        }

    @Test
    fun `상한을 넘겨도 진행 중 작업은 버려지지 않고 다음 호출이 이어받는다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 상한은 기다림만 끊는다. 여기서 작업까지 버리면 매 호출이 처음부터 다시 긁는다.
            val repository = RecordingRepository(gate = CompletableDeferred())
            val coordinator = coordinator(repository = repository)
            coordinator.refresh(timeoutMillis = 1)

            repository.gate?.complete(Unit)
            val second = coordinator.refresh()

            assertEquals(AutoCollectionOutcome.Collected(1), second.outcomes[ItemType.CALENDAR])
            assertEquals(1, repository.addAllCalls)
        }

    @Test
    fun `모두 최신이면 상한 초과와 달리 조용히 통과한다`() =
        runTest(UnconfinedTestDispatcher()) {
            val coordinator = coordinator()
            coordinator.refresh()

            val second = coordinator.refresh(timeoutMillis = 1)

            assertFalse(second.timedOut)
            assertFalse(second.isIncomplete)
        }

    @Test
    fun `세션이 바뀌면 최신성 상태를 버린다`() =
        runTest(UnconfinedTestDispatcher()) {
            // 이전 계정의 `최근 수집함`을 물려받으면 새 계정의 첫 생성이 수집을 건너뛴다.
            val repository = RecordingRepository()
            val coordinator = coordinator(repository = repository)
            coordinator.refresh()

            coordinator.discard()
            runCurrent()
            coordinator.refresh()

            assertEquals(2, repository.addAllCalls)
        }

    private fun TestScope.coordinator(
        availability: CollectionAvailabilityProvider = FakeAvailability(),
        repository: RecordingRepository = RecordingRepository(),
    ) = DefaultAutoCollectionCoordinator(
        availability = availability,
        collectCalendar = CollectCalendarUseCase(mapOf(ItemType.CALENDAR to FakeCollector(ItemType.CALENDAR)), repository),
        collectHealth = CollectHealthUseCase(mapOf(ItemType.HEALTH to FakeCollector(ItemType.HEALTH)), repository),
        applicationScope = backgroundScope,
    )

    /** 실행 중에 권한·지원 상태가 바뀌는 경우를 만든다. */
    private class MutableAvailability(
        var calendar: Boolean = true,
        var healthAvailable: Boolean = true,
        var health: Boolean = true,
    ) : CollectionAvailabilityProvider {
        override fun canCollectCalendar(): Boolean = calendar

        override fun isHealthConnectAvailable(): Boolean = healthAvailable

        override suspend fun canCollectHealth(): Boolean = health
    }

    private class FakeAvailability(
        private val calendar: Boolean = true,
        private val healthAvailable: Boolean = true,
        private val health: Boolean = true,
    ) : CollectionAvailabilityProvider {
        override fun canCollectCalendar(): Boolean = calendar

        override fun isHealthConnectAvailable(): Boolean = healthAvailable

        override suspend fun canCollectHealth(): Boolean = health
    }

    private class FakeCollector(
        override val itemType: ItemType,
    ) : Collector {
        override suspend fun collect(): List<SourceItem> = emptyList()
    }

    private class RecordingRepository(
        private val failCalendar: Boolean = false,
        val gate: CompletableDeferred<Unit>? = null,
    ) : SourceItemRepository {
        var addAllCalls = 0
            private set

        /** 실패해도 시도 자체는 센다 — 재시도 여부를 보려면 성공 횟수만으로는 알 수 없다. */
        var addAllAttempts = 0
            private set

        override suspend fun addAll(items: List<SourceItem>): Int {
            addAllAttempts++
            gate?.await()
            if (failCalendar) throw IllegalStateException("calendar")
            addAllCalls++
            return 1
        }

        override suspend fun upsertAll(items: List<SourceItem>): Int {
            gate?.await()
            return 1
        }

        override fun observeAll(): Flow<List<SourceItem>> = emptyFlow()

        override suspend fun getInWindow(
            start: Instant,
            end: Instant,
        ): List<SourceItem> = emptyList()

        override suspend fun getLatestCollectedAt(itemType: ItemType): Instant? = null

        override suspend fun deleteExpired(cutoff: Instant): Int = 0

        override suspend fun clear(itemType: ItemType) = Unit
    }
}
