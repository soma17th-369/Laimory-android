package com.soma369.laimory.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.soma369.laimory.core.domain.navigation.CollectionPage
import com.soma369.laimory.core.domain.navigation.Feature1Page
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.domain.navigation.SettingsPage
import com.soma369.laimory.core.domain.navigation.TimelinePage
import com.soma369.laimory.feature.collection.screen.CollectionLabRoute
import com.soma369.laimory.feature.feature1.screen.Feature1Route
import com.soma369.laimory.feature.home.screen.HomeRoute
import com.soma369.laimory.feature.timeline.screen.TimelineRoute
import com.soma369.laimory.ui.PlaceholderScreen

/** 바텀바 탭 메타데이터. 탭 루트인 [AppRoute]에만 부여한다. */
data class BottomTab(
    val label: String,
    val icon: ImageVector,
)

/**
 * 앱 한 페이지의 호스트 측 메타데이터.
 *
 * 각 feature 의 typed 인자는 도메인 `Page` 가, 렌더링은 [render] 가 담당한다.
 * 딥링크 부모 체인(`syntheticStack`)은 필요해질 때 확장한다.
 */
data class AppRoute(
    val path: String,
    /** 바텀바 탭 메타데이터. null 이면 일반(push) 화면. */
    val tab: BottomTab? = null,
    /** 페이지 본체 렌더러. args(현재 미사용)를 받아 Composable 을 호출한다. */
    val render: @Composable (innerPadding: PaddingValues, args: Map<String, String>) -> Unit,
) {
    /** 바텀바 탭 루트 여부. 바텀바는 탭 루트에서만 노출한다. */
    val isBottomTab: Boolean get() = tab != null
}

/**
 * 앱의 모든 페이지 메타데이터. 새 화면은 여기에 한 줄 추가한다.
 */
val appRoutes: List<AppRoute> =
    listOf(
        AppRoute(
            path = HomePage.PATH,
            tab = BottomTab(label = "홈", icon = Icons.Filled.Home),
            render = { innerPadding, _ -> HomeRoute(innerPadding = innerPadding) },
        ),
        AppRoute(
            path = TimelinePage.PATH,
            tab = BottomTab(label = "타임라인", icon = Icons.Filled.DateRange),
            render = { innerPadding, _ -> TimelineRoute(innerPadding = innerPadding) },
        ),
        AppRoute(
            path = SettingsPage.PATH,
            tab = BottomTab(label = "설정", icon = Icons.Filled.Settings),
            render = { innerPadding, _ -> PlaceholderScreen(title = "설정", innerPadding = innerPadding) },
        ),
        AppRoute(
            path = CollectionPage.PATH,
            render = { innerPadding, _ -> CollectionLabRoute(innerPadding = innerPadding) },
        ),
        AppRoute(
            path = Feature1Page.PATH,
            render = { innerPadding, _ -> Feature1Route(innerPadding = innerPadding) },
        ),
    )

/** 바텀바에 노출하는 탭 루트 목록. 노출 순서 = 선언 순서. */
val bottomTabRoutes: List<AppRoute> = appRoutes.filter { it.isBottomTab }

/**
 * path → AppRoute 조회 맵. 중복 path는 시작 시점에 fail-fast 한다.
 * (associateBy는 같은 path를 조용히 마지막 항목으로 덮어써 잘못된 화면 회귀를 숨긴다.)
 */
val appRouteByPath: Map<String, AppRoute> =
    buildMap {
        appRoutes.forEach { route ->
            require(put(route.path, route) == null) { "중복된 route path: ${route.path}" }
        }
    }
