package com.soma369.laimory.core.domain.usecase

import com.soma369.laimory.core.domain.provider.LocationAddressResolver
import com.soma369.laimory.core.domain.repository.StayAddressRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveStayAddressUseCaseTest {
    @Test
    fun `해석한 주소를 정리해 돌려주고 같은 rawId에 저장한다`() =
        runTest {
            val repository = RecordingStayAddressRepository(result = true)
            val useCase = ResolveStayAddressUseCase(LocationAddressResolver { _, _ -> "  서울특별시 마포구  " }, repository)

            assertEquals("서울특별시 마포구", useCase(rawId = "stay-1", latitude = 37.5, longitude = 126.9))
            assertEquals("stay-1" to "서울특별시 마포구", repository.updated)
        }

    @Test
    fun `주소를 찾지 못하면 저장하지 않는다`() =
        runTest {
            val repository = RecordingStayAddressRepository(result = true)
            val useCase = ResolveStayAddressUseCase(LocationAddressResolver { _, _ -> null }, repository)

            assertNull(useCase(rawId = "stay-1", latitude = 37.5, longitude = 126.9))
            assertNull(repository.updated)
        }

    @Test
    fun `저장 대상이 사라졌어도 해석한 주소는 돌려준다`() =
        runTest {
            // 저장은 다음 조회를 위한 캐시다. 화면은 이번에 보여줄 주소가 필요하므로 저장 실패에
            // 표시까지 끌려가지 않아야 한다.
            val repository = RecordingStayAddressRepository(result = false)
            val useCase = ResolveStayAddressUseCase(LocationAddressResolver { _, _ -> "서울특별시 마포구" }, repository)

            assertEquals("서울특별시 마포구", useCase(rawId = "stay-1", latitude = 37.5, longitude = 126.9))
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
