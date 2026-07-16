package com.soma369.laimory.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.domain.navigation.NavRoute
import com.soma369.laimory.core.domain.navigation.NavSignal
import com.soma369.laimory.core.util.logging.LogDomain
import com.soma369.laimory.core.util.logging.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * 단일 [GenericNavKey] 디스패처. 실제 화면 결정은 [appRouteByPath]가 담당한다.
 *
 * app이 backStack을 소유하고 [NavDisplay]가 마지막 key를 렌더한다.
 * [navigationFlow]([NavSignal])를 수집해 전진/후진을 backStack 조작으로 매핑한다.
 */
@Composable
fun AppNavHost(
    backStack: NavBackStack<NavKey>,
    innerPadding: PaddingValues,
    navigationFlow: Flow<NavSignal> = emptyFlow(),
) {
    LaunchedEffect(navigationFlow) {
        navigationFlow.collect { signal ->
            when (signal) {
                is NavSignal.GoToDestPage -> backStack.navigateTo(signal.route)
                is NavSignal.ReplaceRoot -> backStack.replaceRoot(signal.route)
                // 루트(마지막 한 개)에서 pop 하면 스택이 비어 NavDisplay가 크래시한다.
                // 시스템 back(onBack)은 NavDisplay가 루트에서 자동으로 막지만, 이 공통 back 채널은
                // 직접 조작이므로 size > 1일 때만 pop 한다(루트 back은 무시).
                NavSignal.Back -> if (backStack.size > 1) backStack.removeLastOrNull()
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        // onBack 기본 pop, predictive back 내장. NavDisplay가 루트에선 호출하지 않는다.
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<GenericNavKey> { navKey ->
                    // path는 문자열 계약이라 오타·미등록·stale 복원이 컴파일을 통과할 수 있다.
                    // 미등록 path는 blank 대신 Home으로 graceful fallback + ERROR 로그로 조기 노출.
                    val route = appRouteByPath[navKey.path]
                    if (route == null) {
                        Logger.e(LogDomain.NAVIGATION, "등록되지 않은 nav path: ${navKey.path} — Home로 fallback (appRoutes 확인)")
                        appRouteByPath.getValue(HomePage.PATH).render(innerPadding, emptyMap())
                        return@entry
                    }
                    route.render(innerPadding, navKey.args)
                }
            },
    )
}

/**
 * [NavRoute] 한 건을 백스택에 push 한다. 미등록 path는 경고 후 무시.
 * 동일 키(path+args)가 스택에 둘 이상 남지 않도록 기존 출현을 제거한 뒤 최상단에 추가한다
 * (Nav3 contentKey는 키별 1회만 유효). 이미 최상단이면 no-op.
 */
private fun NavBackStack<NavKey>.navigateTo(route: NavRoute) {
    if (appRouteByPath[route.path] == null) {
        Logger.w(LogDomain.NAVIGATION, "Unhandled NavRoute: ${route.path}")
        return
    }
    val key = GenericNavKey.of(route)
    if (lastOrNull() == key) return
    removeAll { it == key }
    add(key)
}

/** 인증 경계 전환에서 이전 화면으로 돌아갈 수 없도록 스택을 새 루트 한 건으로 교체한다. */
internal fun NavBackStack<NavKey>.replaceRoot(route: NavRoute) {
    if (appRouteByPath[route.path] == null) {
        Logger.w(LogDomain.NAVIGATION, "Unhandled root NavRoute: ${route.path}")
        return
    }
    val key = GenericNavKey.of(route)
    if (size == 1 && lastOrNull() == key) return
    add(key)
    while (size > 1) {
        removeAt(0)
    }
}

/**
 * 바텀바 탭 전환: 백스택을 [탭 루트] 1개로 교체한다 (탭별 백스택 미보존 정책).
 *
 * push([NavSignal.GoToDestPage])와 달리 이전 화면을 남기지 않으므로, 어느 탭에서든
 * back 은 앱 종료 흐름이 된다. 새 키를 먼저 추가한 뒤 이전 항목을 제거한다 —
 * 반대 순서(clear 후 add)는 스택이 순간적으로 비어 NavDisplay 가 크래시할 수 있다.
 */
internal fun NavBackStack<NavKey>.switchTab(path: String) {
    if (size == 1 && (lastOrNull() as? GenericNavKey)?.path == path) return
    add(GenericNavKey(path))
    while (size > 1) {
        removeAt(0)
    }
}
