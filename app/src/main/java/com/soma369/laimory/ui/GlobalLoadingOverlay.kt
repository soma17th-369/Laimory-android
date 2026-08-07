package com.soma369.laimory.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 앱 전체 입력을 차단하는 전역 로딩 오버레이.
 *
 * 인증 초기화·계정 전환처럼 전역 작업에만 쓰며, 화면 부분 로딩은 각 화면 UiState가 담당한다.
 */
@Composable
internal fun GlobalLoadingOverlay(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return
    // 터치 소비만으로는 시스템 back이 막히지 않으므로 로딩 동안 back도 함께 차단한다.
    BackHandler { }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

private const val SCRIM_ALPHA = 0.32f
