package com.soma369.laimory.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * 앱 하단 탭 바 (app 소유 chrome).
 *
 * 선택 상태를 별도로 들고 있지 않고 backStack top 의 path([currentPath])에서 파생한다.
 * 탭 클릭은 [onTabSelect] 로 위임하고, 실제 backStack 교체는 호스트가 담당한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomBar(
    currentPath: String?,
    onTabSelect: (AppRoute) -> Unit,
) {
    // active=on-surface / inactive=on-surface-variant, 선택 인디케이터(알약)는 투명 처리(디자인 기준).
    val itemColors =
        NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onSurface,
            selectedTextColor = MaterialTheme.colorScheme.onSurface,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicatorColor = Color.Transparent,
        )
    // 탭 클릭 리플(물결) 제거 — 하위 트리 리플 설정을 null 로.
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        Column {
            // 바텀바는 surfaceVariant를 사용하고 구분선으로 콘텐츠 경계를 명확히 한다.
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .navigationBarsPadding(),
            ) {
                NavigationBar(
                    modifier = Modifier.height(BOTTOM_BAR_HEIGHT),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                ) {
                    bottomTabRoutes.forEach { route ->
                        val tab = route.tab ?: return@forEach
                        val selected = route.path == currentPath
                        NavigationBarItem(
                            selected = selected,
                            onClick = { onTabSelect(route) },
                            icon = {
                                Icon(
                                    painter = painterResource(if (selected) tab.activeIcon else tab.inactiveIcon),
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(text = tab.label) },
                            colors = itemColors,
                        )
                    }
                }
            }
        }
    }
}

private val BOTTOM_BAR_HEIGHT = 64.dp
