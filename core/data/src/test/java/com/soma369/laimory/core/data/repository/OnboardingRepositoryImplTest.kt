package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.soma369.laimory.core.data.datasource.remote.OnboardingRemoteDataSource
import com.soma369.laimory.core.domain.model.onboarding.OnboardingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingRepositoryImplTest {
    private val dataStore = InMemoryPreferencesDataStore()
    private val remoteDataSource = RecordingRemoteDataSource()
    private val repository = OnboardingRepositoryImpl(dataStore, remoteDataSource)

    @Test
    fun `저장된 것이 없으면 아직 하지 않은 상태다`() =
        runTest {
            assertEquals(OnboardingState(isCompleted = false, lastPageKey = null), repository.observe().first())
        }

    @Test
    fun `진행 기록은 완료 여부를 건드리지 않는다`() =
        runTest {
            repository.saveProgress("photo")

            val state = repository.observe().first()
            assertEquals("photo", state.lastPageKey)
            assertFalse(state.isCompleted)
        }

    @Test
    fun `서버 기록이 실패해도 로컬 완료는 확정된다`() =
        runTest {
            // 판정의 정본이 설치 단위 로컬이라, 서버가 안 되는 동안 사용자를 온보딩에 묶어 둘 이유가 없다.
            remoteDataSource.failure = IllegalStateException("network")

            repository.complete()

            assertTrue(repository.observe().first().isCompleted)
            assertEquals(1, remoteDataSource.attempts)
        }

    @Test
    fun `완료하면 서버에도 한 번 기록한다`() =
        runTest {
            repository.complete()

            assertEquals(1, remoteDataSource.attempts)
        }

    @Test
    fun `초기화는 서버를 건드리지 않는다`() =
        runTest {
            // 서버에는 false 로 되돌리는 API 가 없다. 설치 단위 판정만 처음으로 간다.
            repository.complete()

            repository.reset()

            assertEquals(1, remoteDataSource.attempts)
        }

    @Test
    fun `완료는 마지막 페이지를 지우지 않는다`() =
        runTest {
            // 완료 뒤에도 설정에서 다시 열 수 있어 진행 흔적을 지울 이유가 없다.
            repository.saveProgress("location")
            repository.complete()

            val state = repository.observe().first()
            assertTrue(state.isCompleted)
            assertEquals("location", state.lastPageKey)
        }

    @Test
    fun `초기화하면 처음 상태로 돌아간다`() =
        runTest {
            repository.saveProgress("calendar")
            repository.complete()

            repository.reset()

            assertEquals(OnboardingState(), repository.observe().first())
            assertEquals(0, dataStore.current.asMap().size)
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

            val state = repository.observe().first()
            assertEquals(null, state.lastPageKey)
            assertTrue(state.isCompleted)
        }

    private class RecordingRemoteDataSource : OnboardingRemoteDataSource {
        var attempts = 0
        var failure: Throwable? = null

        override suspend fun recordCompletion() {
            attempts++
            failure?.let { throw it }
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
