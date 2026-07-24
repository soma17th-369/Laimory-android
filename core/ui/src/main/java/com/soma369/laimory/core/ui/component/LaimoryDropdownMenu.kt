package com.soma369.laimory.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.soma369.laimory.core.ui.R
import com.soma369.laimory.core.ui.theme.LaimoryTheme

/**
 * Laimory 디자인 시스템의 공통 드롭다운 메뉴.
 *
 * [content]에는 [LaimoryDropdownMenuItem]과 [LaimoryDropdownMenuDivider]를 조합해 사용한다.
 */
@Composable
fun LaimoryDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, DropdownMenuVerticalOffset),
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return

    val density = LocalDensity.current
    Popup(
        alignment = Alignment.TopEnd,
        offset =
            with(density) {
                IntOffset(
                    x = offset.x.roundToPx(),
                    y = offset.y.roundToPx(),
                )
            },
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        LaimoryDropdownMenuContent(
            modifier = modifier,
            content = content,
        )
    }
}

@Composable
fun LaimoryDropdownMenuItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: Painter? = null,
    style: LaimoryDropdownMenuItemStyle = LaimoryDropdownMenuItemStyle.Default,
    enabled: Boolean = true,
) {
    val contentColor =
        when (style) {
            LaimoryDropdownMenuItemStyle.Default -> MaterialTheme.colorScheme.onSurface
            LaimoryDropdownMenuItemStyle.Destructive -> MaterialTheme.colorScheme.error
        }
    val enabledAlpha = if (enabled) 1f else DISABLED_CONTENT_ALPHA

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                )
                .padding(DropdownMenuItemPadding)
                .alpha(enabledAlpha),
        horizontalArrangement = Arrangement.spacedBy(DropdownMenuItemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                modifier = Modifier.size(DropdownMenuIconSize),
                painter = leadingIcon,
                contentDescription = null,
                tint = contentColor,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(lineHeight = DropdownMenuItemLineHeight),
            color = contentColor,
        )
    }
}

@Composable
fun LaimoryDropdownMenuDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

enum class LaimoryDropdownMenuItemStyle {
    Default,
    Destructive,
}

@Composable
private fun LaimoryDropdownMenuContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier =
            modifier
                .widthIn(
                    min = DropdownMenuMinWidth,
                    max = DropdownMenuMaxWidth,
                )
                .width(IntrinsicSize.Max),
        shape = RoundedCornerShape(DropdownMenuCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = DropdownMenuShadowElevation,
    ) {
        Column(
            modifier = Modifier.padding(vertical = DropdownMenuVerticalPadding),
            content = content,
        )
    }
}

@Preview(name = "Dropdown menu", showBackground = true, widthDp = 240, heightDp = 180)
@Composable
private fun LaimoryDropdownMenuPreview() {
    LaimoryTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
            contentAlignment = Alignment.TopEnd,
        ) {
            LaimoryDropdownMenuContent {
                LaimoryDropdownMenuItem(
                    label = "수정하기",
                    leadingIcon = painterResource(R.drawable.ico_timeline_tool_edit),
                    onClick = {},
                )
                LaimoryDropdownMenuDivider()
                LaimoryDropdownMenuItem(
                    label = "삭제하기",
                    leadingIcon = painterResource(R.drawable.ico_setting_trash),
                    style = LaimoryDropdownMenuItemStyle.Destructive,
                    onClick = {},
                )
            }
        }
    }
}

private val DropdownMenuMinWidth = 104.dp
private val DropdownMenuMaxWidth = 240.dp
private val DropdownMenuCornerRadius = 12.dp
private val DropdownMenuShadowElevation = 6.dp
private val DropdownMenuVerticalPadding = 4.dp
private val DropdownMenuItemPadding = 8.dp
private val DropdownMenuIconSize = 16.dp
private val DropdownMenuItemSpacing = 10.dp
private val DropdownMenuItemLineHeight = 16.sp
private val DropdownMenuVerticalOffset = 44.dp
private const val DISABLED_CONTENT_ALPHA = 0.38f
