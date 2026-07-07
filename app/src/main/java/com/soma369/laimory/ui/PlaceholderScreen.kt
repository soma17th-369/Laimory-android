package com.soma369.laimory.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 아직 feature 모듈이 없는 탭 목적지를 채우는 임시 화면.
 * 실제 기능 Task에서 feature 모듈이 생기면 해당 Route 로 교체한다.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    innerPadding: PaddingValues,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$title 화면 준비 중",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
