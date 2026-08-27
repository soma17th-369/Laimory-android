package com.soma369.laimory.feature.onboarding.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.theme.Spacing

/**
 * 몇 장 중 몇 번째인지.
 *
 * 점 하나하나는 읽어 줄 것이 없어 전체를 한 덩어리로 묶고 문장으로 대신 읽힌다 — 점 다섯 개를
 * 따로 읽으면 화면 위치만 늘어놓는 소음이 된다.
 */
@Composable
internal fun OnboardingProgress(
    currentIndex: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier =
            modifier.clearAndSetSemantics {
                contentDescription = "${pageCount}단계 중 ${currentIndex + 1}단계"
            },
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentIndex
            Box(
                modifier =
                    Modifier
                        .height(DOT_SIZE)
                        .width(if (isActive) ACTIVE_DOT_WIDTH else DOT_SIZE)
                        .background(if (isActive) activeColor else inactiveColor, CircleShape),
            )
        }
    }
}

private val DOT_SIZE = 8.dp
private val ACTIVE_DOT_WIDTH = 24.dp
