package com.soma369.laimory.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.R
import com.soma369.laimory.core.ui.theme.Spacing

/**
 * Laimory 화면에서 사용하는 중앙 정렬형 상단바.
 *
 * 시스템 상태바 inset은 화면을 소유한 Scaffold가 처리한다. 제목과 우측 액션을 slot으로 열어
 * 문자열 제목뿐 아니라 날짜·복합 제목도 화면 책임으로 구성할 수 있다.
 */
@Composable
fun LaimoryTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(TopAppBarHeight)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = Spacing.extraSmall),
        contentAlignment = Alignment.Center,
    ) {
        if (onBackClick != null) {
            IconButton(
                onClick = onBackClick,
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .size(TopAppBarTouchTarget),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ico_default_caret_left),
                    contentDescription = "뒤로 가기",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(TopAppBarIconSize),
                )
            }
        }

        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = TopAppBarTouchTarget),
                    contentAlignment = Alignment.Center,
                ) {
                    title()
                }
            }
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

private val TopAppBarHeight = 52.dp
private val TopAppBarTouchTarget = 48.dp
private val TopAppBarIconSize = 24.dp
