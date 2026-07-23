package com.soma369.laimory.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing

/**
 * 직접 입력 대신 picker나 dialog를 여는 선택 필드다.
 *
 * [LaimoryTextField]와 동일한 Default / Active / Error / Disabled 시각 규칙을 사용한다.
 */
@Composable
fun LaimorySelectField(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isActive: Boolean = false,
    isError: Boolean = false,
    fieldHeight: Dp = LaimoryFieldDefaultHeight,
    contentAlignment: Alignment = Alignment.CenterStart,
) {
    val visuals =
        laimoryFieldVisuals(
            enabled = enabled,
            isActive = isActive,
            isError = isError,
        )
    Surface(
        modifier =
            modifier
                .height(fieldHeight)
                .alpha(visuals.alpha),
        enabled = enabled,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = visuals.containerColor,
        border = BorderStroke(visuals.borderWidth, visuals.borderColor),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.large),
            contentAlignment = contentAlignment,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = visuals.contentColor,
            )
        }
    }
}

@Preview(name = "SelectField 상태", showBackground = true, widthDp = 360)
@Composable
private fun LaimorySelectFieldPreview() {
    LaimoryTheme {
        Column(
            modifier = Modifier.padding(Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Spacing.large),
        ) {
            LaimorySelectField(value = "오전 08:30", onClick = {})
            LaimorySelectField(value = "오전 08:30", onClick = {}, isActive = true)
            LaimorySelectField(value = "오전 08:30", onClick = {}, isError = true)
            LaimorySelectField(value = "오전 08:30", onClick = {}, enabled = false)
        }
    }
}
