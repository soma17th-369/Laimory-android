package com.soma369.laimory.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.soma369.laimory.core.domain.navigation.CalendarPage
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.domain.navigation.SettingsPage
import org.junit.Assert.assertEquals
import org.junit.Test

class BackStackRestoreTest {
    @Test
    fun `빌드에서 빠진 화면만 남았으면 루트로 바꾼다`() {
        // 회고 탭을 열어 둔 사용자가 그 화면이 빠진 빌드로 올라오는 경우다. 화면은 홈으로 대신
        // 그려지지만 백스택이 그대로면 바텀바가 최상단 path 를 찾지 못해 탭이 사라진다.
        val backStack = NavBackStack<NavKey>(GenericNavKey(REMOVED_PATH))

        backStack.dropUnknownRoutes(HomePage.toRoute())

        assertEquals(listOf(GenericNavKey(HomePage.PATH)), backStack.toList())
    }

    @Test
    fun `가운데 낀 미등록 경로만 걷어낸다`() {
        val backStack =
            NavBackStack<NavKey>(
                GenericNavKey(CalendarPage.PATH),
                GenericNavKey(REMOVED_PATH),
                GenericNavKey(SettingsPage.PATH),
            )

        backStack.dropUnknownRoutes(HomePage.toRoute())

        assertEquals(
            listOf(GenericNavKey(CalendarPage.PATH), GenericNavKey(SettingsPage.PATH)),
            backStack.toList(),
        )
    }

    @Test
    fun `등록된 경로만 있으면 손대지 않는다`() {
        val backStack =
            NavBackStack<NavKey>(
                GenericNavKey(HomePage.PATH),
                GenericNavKey(SettingsPage.PATH),
            )

        backStack.dropUnknownRoutes(HomePage.toRoute())

        assertEquals(
            listOf(GenericNavKey(HomePage.PATH), GenericNavKey(SettingsPage.PATH)),
            backStack.toList(),
        )
    }

    private companion object {
        /** 지금 빌드에 없는 화면. 회고가 여기 해당한다. */
        const val REMOVED_PATH = "/reflection"
    }
}
