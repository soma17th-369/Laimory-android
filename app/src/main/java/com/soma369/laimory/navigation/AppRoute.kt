package com.soma369.laimory.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.soma369.laimory.core.domain.navigation.Feature1Page
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.feature.feature1.screen.Feature1Route
import com.soma369.laimory.feature.home.screen.HomeRoute

/**
 * 앱 한 페이지의 호스트 측 메타데이터.
 *
 * 각 feature 의 typed 인자는 도메인 `Page` 가, 렌더링은 [render] 가 담당한다.
 * 딥링크 부모 체인(`syntheticStack`)·탭 구성은 필요해질 때 확장한다.
 */
data class AppRoute(
    val path: String,
    val isBottomTab: Boolean = false,
    /** 페이지 본체 렌더러. args(현재 미사용)를 받아 Composable 을 호출한다. */
    val render: @Composable (innerPadding: PaddingValues, args: Map<String, String>) -> Unit,
)

/**
 * 앱의 모든 페이지 메타데이터. 새 화면은 여기에 한 줄 추가한다.
 */
val appRoutes: List<AppRoute> =
    listOf(
        AppRoute(
            path = HomePage.PATH,
            render = { innerPadding, _ ->
                // 전진 이동(→ Feature1)은 후속 커밋에서 NavigationHelper 단일 채널로 연결된다.
                HomeRoute(innerPadding = innerPadding, onNavigateToFeature1 = {})
            },
        ),
        AppRoute(
            path = Feature1Page.PATH,
            render = { innerPadding, _ -> Feature1Route(innerPadding = innerPadding) },
        ),
    )

val appRouteByPath: Map<String, AppRoute> = appRoutes.associateBy { it.path }
