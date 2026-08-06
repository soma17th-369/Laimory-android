package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.model.collection.GeoPoint
import com.soma369.laimory.core.domain.provider.LocationAddressResolver
import com.soma369.laimory.core.domain.repository.MovementAddressRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveMovementAddressesUseCaseTest {
    @Test
    fun `출발과 도착 주소를 해석해 한 번에 저장한다`() =
        runTest {
            val repository = RecordingMovementAddressRepository()
            val resolver =
                LocationAddressResolver { latitude, _ ->
                    if (latitude == 37.5) " 출발 주소 " else " 도착 주소 "
                }
            val useCase = ResolveMovementAddressesUseCase(resolver, repository)

            val result =
                useCase(
                    rawId = "move-1",
                    start = GeoPoint(37.5, 126.9),
                    end = GeoPoint(37.6, 127.0),
                )

            assertTrue(result)
            assertEquals(Triple("move-1", "출발 주소", "도착 주소"), repository.updated)
        }

    @Test
    fun `이미 저장된 주소는 다시 해석하지 않고 재사용한다`() =
        runTest {
            var resolveCount = 0
            val repository = RecordingMovementAddressRepository()
            val resolver =
                LocationAddressResolver { _, _ ->
                    resolveCount++
                    "도착 주소"
                }
            val useCase = ResolveMovementAddressesUseCase(resolver, repository)

            useCase(
                rawId = "move-1",
                start = GeoPoint(37.5, 126.9, address = "출발 주소"),
                end = GeoPoint(37.6, 127.0),
            )

            assertEquals(1, resolveCount)
            assertEquals(Triple("move-1", "출발 주소", "도착 주소"), repository.updated)
        }

    private class RecordingMovementAddressRepository : MovementAddressRepository {
        var updated: Triple<String, String?, String?>? = null

        override suspend fun updateAddresses(
            rawId: String,
            startAddress: String?,
            endAddress: String?,
        ): Boolean {
            updated = Triple(rawId, startAddress, endAddress)
            return true
        }
    }
}
