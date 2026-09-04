package com.soma369.laimory.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.terms.TermDocument
import com.soma369.laimory.core.domain.model.terms.TermType
import com.soma369.laimory.core.domain.model.terms.TermsGateState
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.domain.navigation.LoginPage
import com.soma369.laimory.core.domain.navigation.OnboardingPage
import com.soma369.laimory.core.domain.navigation.SettingsPage
import com.soma369.laimory.core.domain.navigation.TermsPage
import com.soma369.laimory.core.domain.navigation.ThemeSettingsPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

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

        backStack.syncRoot(HomePage, appliedRootPath = HomePage.PATH)

        assertEquals(
            listOf(GenericNavKey(HomePage.PATH), GenericNavKey("/collection")),
            backStack.toList(),
        )
    }

    @Test
    fun `홈이 아닌 탭에서 구성 변경이 나도 백스택을 보존한다`() {
        // 탭 전환은 백스택 바닥을 탭 루트로 갈아끼운다. 바닥으로 경계를 판별하면 화면 모드를
        // 고르는 순간(구성 변경) 설정 탭이 홈 한 건으로 날아간다.
        val backStack = NavBackStack<NavKey>(GenericNavKey(HomePage.PATH))
        backStack.switchTab(SettingsPage.PATH)
        backStack.navigateTo(ThemeSettingsPage.toRoute())
        var rootReplaced = false

        backStack.syncRoot(HomePage, appliedRootPath = HomePage.PATH, onRootReplaced = { rootReplaced = true })

        assertEquals(
            listOf(GenericNavKey(SettingsPage.PATH), GenericNavKey(ThemeSettingsPage.PATH)),
            backStack.toList(),
        )
        // 계정이 그대로인데 정리가 돌면 활성 Dialog와 동의 스냅샷까지 사라진다.
        assertFalse(rootReplaced)
    }

    @Test
    fun `인증 경계를 넘을 때만 목표 루트로 교체한다`() {
        val loginBackStack = NavBackStack<NavKey>(GenericNavKey(LoginPage.PATH))
        val authenticatedBackStack =
            NavBackStack<NavKey>(
                GenericNavKey(HomePage.PATH),
                GenericNavKey("/collection"),
            )

        loginBackStack.syncRoot(HomePage, appliedRootPath = LoginPage.PATH)
        authenticatedBackStack.syncRoot(LoginPage, appliedRootPath = HomePage.PATH)

        assertEquals(listOf(GenericNavKey(HomePage.PATH)), loginBackStack.toList())
        assertEquals(listOf(GenericNavKey(LoginPage.PATH)), authenticatedBackStack.toList())
    }

    @Test
    fun `화면이 먼저 루트를 세워 뒀어도 경계가 바뀌면 정리한다`() {
        // 로그아웃은 화면 쪽이 먼저 루트를 갈아 끼우고 세션 흐름이 뒤따라온다. 스택이 이미
        // 목표 루트라도 이전 계정의 Dialog·동의 스냅샷을 정리할 자리는 여기뿐이다.
        val backStack = NavBackStack<NavKey>(GenericNavKey(LoginPage.PATH))
        var rootReplaced = false

        backStack.syncRoot(LoginPage, appliedRootPath = HomePage.PATH, onRootReplaced = { rootReplaced = true })

        assertTrue(rootReplaced)
        assertEquals(listOf(GenericNavKey(LoginPage.PATH)), backStack.toList())
    }

    @Test
    fun `루트가 셋이어도 같은 경계면 백스택을 보존한다`() {
        val onboardingBackStack = NavBackStack<NavKey>(GenericNavKey(OnboardingPage.PATH))

        onboardingBackStack.syncRoot(OnboardingPage, appliedRootPath = OnboardingPage.PATH)

        assertEquals(listOf(GenericNavKey(OnboardingPage.PATH)), onboardingBackStack.toList())
    }

    @Test
    fun `온보딩에서 홈으로 넘어갈 때 루트를 교체한다`() {
        val backStack = NavBackStack<NavKey>(GenericNavKey(OnboardingPage.PATH))

        backStack.syncRoot(HomePage, appliedRootPath = OnboardingPage.PATH)

        assertEquals(listOf(GenericNavKey(HomePage.PATH)), backStack.toList())
    }

    @Test
    fun `세션 확인 중에는 화면을 정하지 않는다`() {
        assertNull(rootPage(AuthSessionState.Loading, TermsGateState.Satisfied, onboardingCompleted = true))
    }

    @Test
    fun `완료 여부를 모르는 동안에는 홈을 먼저 보여주지 않는다`() {
        // 서버 조회 전이라는 뜻이다. 여기서 홈을 내주면 온보딩이 필요한 사용자에게 홈이 한 프레임 번쩍인다.
        assertNull(rootPage(AuthSessionState.Authenticated, TermsGateState.Satisfied, onboardingCompleted = null))
    }

    @Test
    fun `약관 판정 전에는 어느 화면도 열지 않는다`() {
        // 이용약관에 동의하지 않으면 서버가 인증 API 대부분을 막는다. 모른 채 홈을 그리면
        // 오류만 뜨는 화면을 먼저 보게 된다.
        assertNull(rootPage(AuthSessionState.Authenticated, TermsGateState.Unknown, onboardingCompleted = true))
    }

    @Test
    fun `로그인하지 않았으면 완료 여부를 기다리지 않는다`() {
        // 로그인 화면은 온보딩과 무관하다. 기다리면 로그인만 늦게 뜬다.
        assertEquals(
            LoginPage,
            rootPage(AuthSessionState.Unauthenticated, TermsGateState.Unknown, onboardingCompleted = null),
        )
    }

    @Test
    fun `이용약관 미동의는 온보딩보다 앞선다`() {
        // 온보딩을 이미 마친 계정도 미동의면 여기부터 지나야 한다. 온보딩은 설치 단위이고
        // 약관 동의는 계정 단위라 두 상태를 섞을 수 없다.
        assertEquals(
            TermsPage,
            rootPage(
                AuthSessionState.Authenticated,
                TermsGateState.Required(listOf(termsOfService())),
                onboardingCompleted = true,
            ),
        )
    }

    @Test
    fun `약관 조회 실패도 약관 화면이 받는다`() {
        // 홈으로 열어 주면 이후 API 가 계속 거절당하고, 계속 기다리면 로딩에서 나오지 못한다.
        assertEquals(
            TermsPage,
            rootPage(AuthSessionState.Authenticated, TermsGateState.Failed, onboardingCompleted = true),
        )
    }

    @Test
    fun `약관을 통과하면 온보딩 완료 여부로 갈린다`() {
        assertEquals(
            OnboardingPage,
            rootPage(AuthSessionState.Authenticated, TermsGateState.Satisfied, onboardingCompleted = false),
        )
        assertEquals(
            HomePage,
            rootPage(AuthSessionState.Authenticated, TermsGateState.Satisfied, onboardingCompleted = true),
        )
    }

    private fun termsOfService() =
        TermDocument(
            termType = TermType.TERMS_OF_SERVICE,
            version = "1.0",
            title = "라이모리 이용약관",
            contentUrl = "https://laimory.app/terms/terms-of-service/1.0",
            effectiveAt = LocalDateTime.of(2026, 8, 28, 0, 0),
        )
}
