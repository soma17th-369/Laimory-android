package com.soma369.laimory.core.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
internal data class LaimoryFieldVisuals(
    val labelColor: Color,
    val borderColor: Color,
    val borderWidth: Dp,
    val containerColor: Color,
    val supportingColor: Color,
    val contentColor: Color,
    /**
     * 비어 있을 때의 안내 문구 색. 입력한 글자([contentColor])와 달리 포커스·오류에 따라 진해지지 않는다 —
     * 포커스가 잡힌 빈 칸에서 안내 문구가 입력한 글자처럼 보이면 무엇을 썼는지 헷갈린다.
     */
    val placeholderColor: Color,
    val alpha: Float,
)

@Composable
internal fun laimoryFieldVisuals(
    enabled: Boolean,
    isActive: Boolean,
    isError: Boolean,
): LaimoryFieldVisuals =
    LaimoryFieldVisuals(
        labelColor =
            when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                isError -> MaterialTheme.colorScheme.error
                isActive -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        borderColor =
            when {
                !enabled -> MaterialTheme.colorScheme.outlineVariant
                isError -> MaterialTheme.colorScheme.error
                isActive -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            },
        borderWidth =
            if (isActive && enabled && !isError) {
                LaimoryFieldFocusedBorderWidth
            } else {
                LaimoryFieldDefaultBorderWidth
            },
        containerColor =
            if (enabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        supportingColor =
            if (isError && enabled) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        contentColor =
            when {
                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                isActive || isError -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        alpha = if (enabled) LAIMORY_FIELD_ENABLED_ALPHA else LAIMORY_FIELD_DISABLED_ALPHA,
    )

internal val LaimoryFieldDefaultHeight = 48.dp
internal val LaimoryFieldDefaultBorderWidth = 1.5.dp
internal val LaimoryFieldFocusedBorderWidth = 2.dp
internal const val LAIMORY_FIELD_ENABLED_ALPHA = 1f
internal const val LAIMORY_FIELD_DISABLED_ALPHA = 0.6f
