package com.soma369.laimory.feature.onboarding.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

/**
 * 몇 장 중 몇 번째인지.
 *
 * 장마다 칸 하나를 주고 **지나온 칸까지 채운다** — 점 하나만 굵어지는 표시는 남은 길이를 말해
 * 주지 못한다. 주어진 폭을 칸들이 똑같이 나눠 가지므로 장 수가 늘어도 배치가 그대로다.
 *
 * 칸 하나하나는 읽어 줄 것이 없어 전체를 한 덩어리로 묶고 문장으로 대신 읽힌다 — 칸 일곱 개를
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
        horizontalArrangement = Arrangement.spacedBy(SEGMENT_GAP),
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(SEGMENT_HEIGHT)
                        .background(if (index <= currentIndex) activeColor else inactiveColor, CircleShape),
            )
        }
    }
}

private val SEGMENT_HEIGHT = 6.dp
private val SEGMENT_GAP = 4.dp
