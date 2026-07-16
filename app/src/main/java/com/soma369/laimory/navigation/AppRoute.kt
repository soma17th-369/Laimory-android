package com.soma369.laimory.navigation

import androidx.annotation.DrawableRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.soma369.laimory.core.domain.navigation.CalendarPage
import com.soma369.laimory.core.domain.navigation.CollectionPage
import com.soma369.laimory.core.domain.navigation.Feature1Page
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.domain.navigation.LoginPage
import com.soma369.laimory.core.domain.navigation.ReflectionPage
import com.soma369.laimory.core.domain.navigation.SettingsPage
import com.soma369.laimory.feature.collection.screen.CollectionLabRoute
import com.soma369.laimory.feature.feature1.screen.Feature1Route
import com.soma369.laimory.feature.home.screen.HomeRoute
import com.soma369.laimory.feature.login.screen.LoginRoute
import com.soma369.laimory.feature.timeline.screen.TimelineRoute
import com.soma369.laimory.ui.PlaceholderScreen
import com.soma369.laimory.core.ui.R as UiR

/**
 * 바텀바 탭 메타데이터. 탭 루트인 [AppRoute]에만 부여한다.
 *
 * 아이콘은 선택/비선택 상태별 드로어블 쌍이다(active=채움꼴, inactive=외곽선꼴). 색은 리소스에
 * 박힌 값 대신 NavigationBarItem 의 content color 로 틴트되어 다크/라이트 모드에 자동 대응한다.
 */
data class BottomTab(
    val label: String,
    @DrawableRes val activeIcon: Int,
    @DrawableRes val inactiveIcon: Int,
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
            path = LoginPage.PATH,
            render = { innerPadding, _ -> LoginAppRoute(innerPadding) },
        ),
        AppRoute(
            path = HomePage.PATH,
            tab =
                BottomTab(
                    label = "홈",
                    activeIcon = UiR.drawable.ico_bot_nav_active_home,
                    inactiveIcon = UiR.drawable.ico_bot_nav_inactive_home,
                ),
            render = { innerPadding, _ -> HomeRoute(innerPadding = innerPadding) },
        ),
        AppRoute(
            path = CalendarPage.PATH,
            tab =
                BottomTab(
                    label = "캘린더",
                    activeIcon = UiR.drawable.ico_bot_nav_active_calendar,
                    inactiveIcon = UiR.drawable.ico_bot_nav_inactive_calendar,
                ),
            // 캘린더 화면이 준비되기 전까지 타임라인 셸을 이 탭에서 임시 호스팅한다.
            render = { innerPadding, _ -> TimelineRoute(innerPadding = innerPadding) },
        ),
        AppRoute(
            path = ReflectionPage.PATH,
            tab =
                BottomTab(
                    label = "회고",
                    activeIcon = UiR.drawable.ico_bot_nav_active_reflection,
                    inactiveIcon = UiR.drawable.ico_bot_nav_inactive_reflection,
                ),
            render = { innerPadding, _ -> PlaceholderScreen(title = "회고", innerPadding = innerPadding) },
        ),
        AppRoute(
            path = SettingsPage.PATH,
            tab =
                BottomTab(
                    label = "설정",
                    activeIcon = UiR.drawable.ico_bot_nav_active_settings,
                    inactiveIcon = UiR.drawable.ico_bot_nav_inactive_settings,
                ),
            render = { innerPadding, _ -> PlaceholderScreen(title = "설정", innerPadding = innerPadding) },
        ),
        // 아래는 바텀바에 노출하지 않는 non-tab 루트(테스트/디버그 진입점 보존).
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

@Composable
private fun LoginAppRoute(innerPadding: PaddingValues) {
    val context = LocalContext.current
    LoginRoute(
        innerPadding = innerPadding,
        onOpenAuthorizationUrl = { url ->
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .build()
                .launchUrl(context, url.toUri())
        },
    )
}
