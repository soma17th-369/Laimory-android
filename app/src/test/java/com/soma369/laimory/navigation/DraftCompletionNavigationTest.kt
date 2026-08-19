package com.soma369.laimory.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.soma369.laimory.core.domain.navigation.DraftLoadingPage
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.domain.navigation.TimelinePage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DraftCompletionNavigationTest {
    private val recordDate = LocalDate.of(2026, 8, 18)
    private val timelineRoute = TimelinePage(recordDate).toRoute()

    @Test
    fun `로딩 화면을 보고 있으면 자동 이동 대상으로 판정한다`() {
        val backStack = NavBackStack<NavKey>(GenericNavKey(HomePage.PATH), GenericNavKey(DraftLoadingPage.PATH))

        assertTrue(backStack.isShowingDraftLoading())
    }

    @Test
    fun `다른 화면을 보고 있으면 자동 이동하지 않는다`() {
        val backStack = NavBackStack<NavKey>(GenericNavKey(HomePage.PATH), GenericNavKey("/collection"))

        assertFalse(backStack.isShowingDraftLoading())
    }

    @Test
    fun `완료하면 로딩 화면을 빼고 타임라인을 올린다`() {
        val backStack = NavBackStack<NavKey>(GenericNavKey(HomePage.PATH), GenericNavKey(DraftLoadingPage.PATH))

        backStack.replaceTopWith(timelineRoute)

        // 다 만들어진 뒤 back 으로 로딩 화면에 되돌아가지 않는다.
        assertEquals(GenericNavKey(HomePage.PATH), backStack.first())
        assertEquals(2, backStack.size)
        assertFalse(backStack.isShowingDraftLoading())
    }

    @Test
    fun `루트 한 건만 남았으면 빼지 않는다`() {
        val backStack = NavBackStack<NavKey>(GenericNavKey(DraftLoadingPage.PATH))

        backStack.replaceTopWith(timelineRoute)

        // 스택이 비면 NavDisplay 가 크래시한다.
        assertEquals(2, backStack.size)
        assertEquals(GenericNavKey(DraftLoadingPage.PATH), backStack.first())
    }
}
