package com.soma369.laimory.feature.collection.viewmodel

import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.tutorial.Tutorial
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.repository.TutorialRepository
import com.soma369.laimory.core.domain.usecase.tutorial.ResetTutorialsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class TutorialResetViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeTutorialRepository()
    private val navigationHelper = RecordingNavigationHelper()
    private val viewModel = TutorialResetViewModel(ResetTutorialsUseCase(repository), navigationHelper)

    @Test
    fun `튜토리얼을 되돌리고 곧장 홈으로 간다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            viewModel.reset()
            runCurrent()

            assertEquals(1, repository.resetCount)
            assertEquals(HomePage, navigationHelper.replacedRoot)
        }

    @Test
    fun `되돌리지 못하면 홈으로 가지 않는다`() =
        runTest(mainDispatcherRule.testDispatcher) {
            repository.failure = IOException("broken")

            viewModel.reset()
            runCurrent()

            assertNull(navigationHelper.replacedRoot)
        }

    private class FakeTutorialRepository : TutorialRepository {
        var resetCount = 0
        var failure: Exception? = null

        override fun observeSeen(tutorial: Tutorial): Flow<Boolean> = flowOf(true)

        override suspend fun markSeen(tutorial: Tutorial) = Unit

        override suspend fun resetAll() {
            failure?.let { throw it }
            resetCount++
        }
    }

    private class RecordingNavigationHelper : NavigationHelper {
        var replacedRoot: Page? = null

        override fun navigateTo(page: Page) = Unit

        override fun replaceRoot(page: Page) {
            replacedRoot = page
        }

        override fun navigateToBack() = Unit
    }
}
