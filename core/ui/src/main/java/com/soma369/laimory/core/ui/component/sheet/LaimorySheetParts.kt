package com.soma369.laimory.core.ui.component.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.R
import com.soma369.laimory.core.ui.theme.Spacing

/**
 * 바텀시트 손잡이. Figma 시트들이 공유하는 규격이라 시트마다 다시 그리지 않는다.
 *
 * [ModalBottomSheet]의 `dragHandle` 슬롯에 그대로 넘겨 쓴다.
 */
@Composable
fun LaimorySheetDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.extraLarge2),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .width(DragHandleWidth)
                    .height(DragHandleHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(percent = 50)),
        )
    }
}

/**
 * 바텀시트 제목 줄 — 왼쪽 제목, 오른쪽 닫기.
 *
 * [verticalPadding]을 열어 둔 것은 시트가 펼침 상태에 따라 세로 여유를 줄이기 때문이고,
 * [closeEnabled]는 요청이 진행 중이라 닫기를 막아야 하는 시트를 위한 것이다.
 */
@Composable
fun LaimorySheetHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    verticalPadding: Dp = Spacing.small,
    closeEnabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = verticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        // 아이콘은 24dp지만 누를 수 있는 영역은 최소 터치 크기를 지켜야 한다.
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(CloseTouchTargetSize).offset(x = CloseIconInset),
            enabled = closeEnabled,
        ) {
            Icon(
                painter = painterResource(R.drawable.ico_default_close),
                contentDescription = "닫기",
                modifier = Modifier.size(CloseIconSize),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private val DragHandleWidth = 40.dp
private val DragHandleHeight = 4.dp
private val CloseIconSize = 24.dp
private val CloseTouchTargetSize = 48.dp

// 터치 영역을 넓혀도 아이콘은 Figma대로 시트 오른쪽 끝에 맞춘다.
private val CloseIconInset = 12.dp
