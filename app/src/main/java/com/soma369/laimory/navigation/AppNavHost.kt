package com.soma369.laimory.navigation

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay

/**
 * 단일 [GenericNavKey] 디스패처. 실제 화면 결정은 [appRouteByPath] 가 담당한다.
 *
 * app 이 backStack 을 소유하고 [NavDisplay] 가 마지막 key 를 렌더한다.
 * 미등록 path 는 무시 + 경고 로그.
 */
@Composable
fun AppNavHost(
    backStack: NavBackStack<NavKey>,
    innerPadding: PaddingValues,
) {
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

private const val TAG = "[Navigation]"
