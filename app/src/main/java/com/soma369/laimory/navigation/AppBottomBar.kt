package com.soma369.laimory.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * 앱 하단 탭 바 (app 소유 chrome).
 *
 * 선택 상태를 별도로 들고 있지 않고 backStack top 의 path([currentPath])에서 파생한다.
 * 탭 클릭은 [onTabSelect] 로 위임하고, 실제 backStack 교체는 호스트가 담당한다.
 */
@Composable
fun AppBottomBar(
    currentPath: String?,
    onTabSelect: (AppRoute) -> Unit,
) {
    NavigationBar {
        bottomTabRoutes.forEach { route ->
            val tab = route.tab ?: return@forEach
            NavigationBarItem(
                selected = route.path == currentPath,
                onClick = { onTabSelect(route) },
                icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                label = { Text(text = tab.label) },
            )
        }
    }
}
