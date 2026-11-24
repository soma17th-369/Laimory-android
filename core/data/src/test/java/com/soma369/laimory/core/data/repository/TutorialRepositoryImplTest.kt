package com.soma369.laimory.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.soma369.laimory.core.domain.model.tutorial.Tutorial
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class TutorialRepositoryImplTest {
    private val dataStore = InMemoryPreferencesDataStore()
    private val repository = TutorialRepositoryImpl(dataStore)

    @Test
    fun `저장한 적이 없으면 보지 않은 것이다`() =
        runTest {
            assertFalse(repository.observeSeen(Tutorial.HOME).first())
        }

    @Test
    fun `봤다고 남기면 본 것이다`() =
        runTest {
            repository.markSeen(Tutorial.HOME)

            assertTrue(repository.observeSeen(Tutorial.HOME).first())
        }

    @Test
    fun `초기화하면 다시 보지 않은 것이다`() =
        runTest {
            repository.markSeen(Tutorial.HOME)

            repository.resetAll()

            assertFalse(repository.observeSeen(Tutorial.HOME).first())
        }

    @Test
    fun `다시 시작하면 보지 않은 것이 되고 다시 시작했다고 남는다`() =
        runTest {
            repository.markSeen(Tutorial.HOME)

            repository.restartAll()

            assertFalse(repository.observeSeen(Tutorial.HOME).first())
            assertTrue(repository.isRestarted(Tutorial.HOME))
            assertTrue(repository.isRestarted(Tutorial.TIMELINE))
        }

    @Test
    fun `다시 시작한 튜토리얼을 마치면 그 튜토리얼의 표시만 걷힌다`() =
        runTest {
            repository.restartAll()

            repository.markSeen(Tutorial.HOME)

            assertFalse(repository.isRestarted(Tutorial.HOME))
            assertTrue(repository.isRestarted(Tutorial.TIMELINE))
        }

    @Test
    fun `초기화는 다시 시작한 표시도 걷어 처음 보는 것으로 돌린다`() =
        runTest {
            repository.restartAll()

            repository.resetAll()

            assertFalse(repository.isRestarted(Tutorial.HOME))
            assertFalse(repository.observeSeen(Tutorial.HOME).first())
        }

    @Test
    fun `읽지 못하면 본 것으로 본다`() =
        runTest {
            val broken = TutorialRepositoryImpl(BrokenDataStore())

            assertTrue(broken.observeSeen(Tutorial.HOME).first())
            assertFalse(broken.isRestarted(Tutorial.HOME))
        }

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(emptyPreferences())
        private val mutex = Mutex()

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            mutex.withLock {
                transform(state.value).also { state.value = it }
            }
    }

    private class BrokenDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw IOException("broken") }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = throw IOException("broken")
    }
}
