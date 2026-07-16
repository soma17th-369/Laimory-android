package com.soma369.laimory.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.domain.navigation.LoginPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthNavigationTest {
    @Test
    fun `루트 교체는 이전 인증 화면 이력을 모두 제거한다`() {
        val backStack =
            NavBackStack<NavKey>(
                GenericNavKey(HomePage.PATH),
                GenericNavKey("/collection"),
            )

        backStack.replaceRoot(LoginPage.toRoute())

        assertEquals(listOf(GenericNavKey(LoginPage.PATH)), backStack.toList())
    }

    @Test
    fun `같은 루트 교체는 중복 key를 만들지 않는다`() {
        val backStack = NavBackStack<NavKey>(GenericNavKey(LoginPage.PATH))

        backStack.replaceRoot(LoginPage.toRoute())

        assertEquals(1, backStack.size)
    }

    @Test
    fun `인증 경계가 유지되면 구성 변경으로 복원된 백스택을 보존한다`() {
        val backStack =
            NavBackStack<NavKey>(
                GenericNavKey(HomePage.PATH),
                GenericNavKey("/collection"),
            )

        backStack.syncAuthRoot(HomePage)

        assertEquals(
            listOf(GenericNavKey(HomePage.PATH), GenericNavKey("/collection")),
            backStack.toList(),
        )
    }

    @Test
    fun `인증 경계를 넘을 때만 목표 루트로 교체한다`() {
        val loginBackStack = NavBackStack<NavKey>(GenericNavKey(LoginPage.PATH))
        val authenticatedBackStack =
            NavBackStack<NavKey>(
                GenericNavKey(HomePage.PATH),
                GenericNavKey("/collection"),
            )

        loginBackStack.syncAuthRoot(HomePage)
        authenticatedBackStack.syncAuthRoot(LoginPage)

        assertEquals(listOf(GenericNavKey(HomePage.PATH)), loginBackStack.toList())
        assertEquals(listOf(GenericNavKey(LoginPage.PATH)), authenticatedBackStack.toList())
    }

    @Test
    fun `세션 게이트는 확인 중에는 화면을 정하지 않고 인증 여부에 맞는 루트를 고른다`() {
        assertNull(AuthSessionState.Loading.rootPage())
        assertEquals(HomePage, AuthSessionState.Authenticated.rootPage())
        assertEquals(LoginPage, AuthSessionState.Unauthenticated.rootPage())
    }
}
