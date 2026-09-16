package com.soma369.laimory.feature.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.R as UiR

/**
 * 사진 선택 표시. 고른 사진은 파란 원 안의 흰 체크다.
 *
 * 고르지 않은 사진에는 흰 테두리의 빈 원을 둬 누를 자리를 알린다. 사진 위에 얹히므로 어떤 사진에서도
 * 보이도록 반투명한 어두운 바탕을 깐다. 낭독은 이 배지를 감싼 쪽이 맡는다.
 */
@Composable
internal fun PhotoCheckBadge(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
) {
    val base = modifier.size(size)
    if (selected) {
        Box(
            modifier = base.background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(UiR.drawable.ico_default_check),
                contentDescription = null,
                modifier = Modifier.size(size * CHECK_TO_BADGE_RATIO),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    } else {
        Box(
            modifier =
                base
                    .background(Color.Black.copy(alpha = UNSELECTED_FILL_ALPHA), CircleShape)
                    .border(1.5.dp, Color.White, CircleShape),
        )
    }
}

private const val CHECK_TO_BADGE_RATIO = 0.5f
private const val UNSELECTED_FILL_ALPHA = 0.2f
