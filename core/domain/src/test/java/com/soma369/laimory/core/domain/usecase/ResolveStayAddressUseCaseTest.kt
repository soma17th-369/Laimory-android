package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.provider.LocationAddressResolver
import com.soma369.laimory.core.domain.repository.StayAddressRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolveStayAddressUseCaseTest {
    @Test
    fun `해석한 주소를 정리해 같은 rawId에 저장한다`() =
        runTest {
            val repository = RecordingStayAddressRepository(result = true)
            val useCase = ResolveStayAddressUseCase(LocationAddressResolver { _, _ -> "  서울특별시 마포구  " }, repository)

            assertTrue(useCase(rawId = "stay-1", latitude = 37.5, longitude = 126.9))
            assertEquals("stay-1" to "서울특별시 마포구", repository.updated)
        }

    @Test
    fun `주소를 찾지 못하면 저장하지 않는다`() =
        runTest {
            val repository = RecordingStayAddressRepository(result = true)
            val useCase = ResolveStayAddressUseCase(LocationAddressResolver { _, _ -> null }, repository)

            assertFalse(useCase(rawId = "stay-1", latitude = 37.5, longitude = 126.9))
            assertEquals(null, repository.updated)
        }

    private class RecordingStayAddressRepository(
        private val result: Boolean,
    ) : StayAddressRepository {
        var updated: Pair<String, String>? = null

        override suspend fun updateAddress(
            rawId: String,
            address: String,
        ): Boolean {
            updated = rawId to address
            return result
        }
    }
}
