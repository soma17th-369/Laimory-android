package com.soma369.laimory.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.domain.navigation.LoginPage
import com.soma369.laimory.core.domain.navigation.OnboardingPage
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

        backStack.syncRoot(HomePage)

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

        loginBackStack.syncRoot(HomePage)
        authenticatedBackStack.syncRoot(LoginPage)

        assertEquals(listOf(GenericNavKey(HomePage.PATH)), loginBackStack.toList())
        assertEquals(listOf(GenericNavKey(LoginPage.PATH)), authenticatedBackStack.toList())
    }

    @Test
    fun `루트가 셋이어도 바닥 경로가 같으면 백스택을 보존한다`() {
        val onboardingBackStack = NavBackStack<NavKey>(GenericNavKey(OnboardingPage.PATH))

        onboardingBackStack.syncRoot(OnboardingPage)

        assertEquals(listOf(GenericNavKey(OnboardingPage.PATH)), onboardingBackStack.toList())
    }

    @Test
    fun `온보딩에서 홈으로 넘어갈 때 루트를 교체한다`() {
        val backStack = NavBackStack<NavKey>(GenericNavKey(OnboardingPage.PATH))

        backStack.syncRoot(HomePage)

        assertEquals(listOf(GenericNavKey(HomePage.PATH)), backStack.toList())
    }

    @Test
    fun `세션 확인 중에는 화면을 정하지 않는다`() {
        assertNull(rootPage(AuthSessionState.Loading, onboardingCompleted = true))
    }

    @Test
    fun `완료 여부를 모르는 동안에는 홈을 먼저 보여주지 않는다`() {
        // 서버 조회 전이라는 뜻이다. 여기서 홈을 내주면 온보딩이 필요한 사용자에게 홈이 한 프레임 번쩍인다.
        assertNull(rootPage(AuthSessionState.Authenticated, onboardingCompleted = null))
    }

    @Test
    fun `로그인하지 않았으면 완료 여부를 기다리지 않는다`() {
        // 로그인 화면은 온보딩과 무관하다. 기다리면 로그인만 늦게 뜬다.
        assertEquals(LoginPage, rootPage(AuthSessionState.Unauthenticated, onboardingCompleted = null))
    }

    @Test
    fun `인증된 사용자는 온보딩 완료 여부로 갈린다`() {
        assertEquals(
            OnboardingPage,
            rootPage(AuthSessionState.Authenticated, onboardingCompleted = false),
        )
        assertEquals(
            HomePage,
            rootPage(AuthSessionState.Authenticated, onboardingCompleted = true),
        )
    }
}
