package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.soma369.laimory.core.data.datasource.remote.OnboardingRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingRepositoryImplTest {
    private val dataStore = InMemoryPreferencesDataStore()
    private val remoteDataSource = FakeRemoteDataSource()
    private val repository = OnboardingRepositoryImpl(dataStore, remoteDataSource)

    @Test
    fun `받은 적이 없으면 캐시는 모름이다`() =
        runTest {
            // false 로 떨어뜨리면 "완료 아님" 과 "아직 못 받음" 을 조율자가 구분하지 못한다.
            assertNull(repository.cachedCompletion())
        }

    @Test
    fun `서버 값을 캐시에 남긴다`() =
        runTest {
            repository.cacheCompletion(true)

            assertEquals(true, repository.cachedCompletion())
        }

    @Test
    fun `조회 실패는 예외가 아니라 값으로 돌아온다`() =
        runTest {
            // 앱 진입 판정에 쓰이므로 예외를 올리면 루트를 못 정해 로딩에서 멈춘다.
            remoteDataSource.fetchFailure = IllegalStateException("500")

            val result = repository.fetchCompletion()

            assertTrue(result.isFailure)
        }

    @Test
    fun `진행 기록은 캐시를 건드리지 않는다`() =
        runTest {
            repository.saveProgress("photo")

            assertEquals("photo", repository.observeLastPageKey().first())
            assertNull(repository.cachedCompletion())
        }

    @Test
    fun `공백만 있는 페이지 키는 없는 것으로 본다`() =
        runTest {
            dataStore.updateData {
                mutablePreferencesOf(
                    stringPreferencesKey("last_page_key") to "   ",
                    booleanPreferencesKey("is_completed") to true,
                )
            }

            assertNull(repository.observeLastPageKey().first())
            assertEquals(true, repository.cachedCompletion())
        }

    @Test
    fun `비우면 캐시와 진행 위치가 함께 사라진다`() =
        runTest {
            // 계정 경계에서 부른다. 캐시만 남으면 이전 계정의 완료가 새 계정으로 샌다.
            repository.cacheCompletion(true)
            repository.saveProgress("calendar")

            repository.clear()

            assertNull(repository.cachedCompletion())
            assertNull(repository.observeLastPageKey().first())
            assertEquals(0, dataStore.current.asMap().size)
        }

    @Test
    fun `완료 기록은 서버로 나간다`() =
        runTest {
            repository.recordCompletion()

            assertEquals(1, remoteDataSource.recordAttempts)
        }

    @Test
    fun `조회 성공은 서버 값을 그대로 돌려준다`() =
        runTest {
            remoteDataSource.completion = false

            assertFalse(repository.fetchCompletion().getOrThrow())
        }

    private class FakeRemoteDataSource : OnboardingRemoteDataSource {
        var recordAttempts = 0
        var completion = true
        var fetchFailure: Throwable? = null

        override suspend fun recordCompletion() {
            recordAttempts++
        }

        override suspend fun fetchCompletion(): Boolean {
            fetchFailure?.let { throw it }
            return completion
        }
    }

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(emptyPreferences())
        private val mutex = Mutex()

        val current: Preferences get() = state.value

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            mutex.withLock {
                transform(state.value).also { state.value = it }
            }
    }
}
