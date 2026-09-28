package com.soma369.laimory.feature.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** 아직 눌러 보지 않은 새 공지 표시. 설정의 `공지사항` 줄과 공지 목록이 같은 점을 쓴다. */
@Composable
fun NewNoticeDot(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .size(6.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .semantics { contentDescription = "새 공지" },
    )
}
