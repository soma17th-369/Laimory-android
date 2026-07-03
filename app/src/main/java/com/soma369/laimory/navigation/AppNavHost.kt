package com.soma369.laimory.navigation

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.soma369.laimory.core.domain.navigation.NavRoute
import com.soma369.laimory.core.domain.navigation.NavSignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * 단일 [GenericNavKey] 디스패처. 실제 화면 결정은 [appRouteByPath]가 담당한다.
 *
 * app이 backStack을 소유하고 [NavDisplay]가 마지막 key를 렌더한다.
 * [navigationFlow]([NavSignal])를 수집해 전진/후진을 backStack 조작으로 매핑한다. 미등록 path는 무시 + 경고 로그.
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
                NavSignal.Back -> backStack.removeLastOrNull()
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        // onBack 기본 pop, predictive back 내장.
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<GenericNavKey> { navKey ->
                    val route = appRouteByPath[navKey.path]
                    if (route == null) {
                        Log.w(TAG, "Unknown path on render: ${navKey.path}")
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
        Log.w(TAG, "Unhandled NavRoute: ${route.path}")
        return
    }
    val key = GenericNavKey.of(route)
    if (lastOrNull() == key) return
    removeAll { it == key }
    add(key)
}

private const val TAG = "[Navigation]"
