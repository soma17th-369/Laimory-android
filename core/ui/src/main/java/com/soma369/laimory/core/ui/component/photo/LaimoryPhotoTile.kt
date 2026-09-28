package com.soma369.laimory.core.ui.component.photo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.R
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing

object LaimoryPhotoTileDefaults {
    /** 타임라인 이벤트 편집 시안의 사진 칸 크기. */
    val Size: Dp = 64.dp
}

/**
 * 사진을 붙이는 자리의 사진 한 칸. 앱 안에서 사진을 붙이는 자리는 이 모양 하나다.
 *
 * `core:ui` 는 이미지 로더에 의존하지 않는다 — 사진은 [image] 로 호출부가 그린다(보통 Coil `AsyncImage`).
 * 칸에 덧씌울 상태(업로드 중·실패)는 [overlay] 로 받는다.
 *
 * 빼기 버튼은 반전 색(inverseSurface 위 inverseOnSurface) 원에 X(Figma `icon/cancel`)다. 사진 위에 뜨므로
 * 사진 밝기와 무관하게 보여야 하고, 반전 색이라 라이트·다크 어느 쪽에서도 대비가 난다. 빼는 일이
 * 첨부 해제든 저장된 사진 제거든 아이콘은 하나로 둔다 — 무엇이 일어나는지는 호출부의 확인창이 말한다.
 *
 * @param onRemove 없으면 빼기 버튼을 그리지 않는다(보내는 중처럼 손댈 수 없을 때).
 */
@Composable
fun LaimoryPhotoTile(
    modifier: Modifier = Modifier,
    removeContentDescription: String? = null,
    onRemove: (() -> Unit)? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
    image: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .size(LaimoryPhotoTileDefaults.Size)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .photoTileBorder(MaterialTheme.colorScheme.outlineVariant),
    ) {
        image()
        overlay()
        if (onRemove != null) {
            Surface(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(REMOVE_BUTTON_SIZE),
                onClick = onRemove,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.inverseSurface,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ico_default_close),
                        contentDescription = removeContentDescription,
                        modifier = Modifier.size(REMOVE_ICON_SIZE),
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }
            }
        }
    }
}

/** 사진 칸 줄 끝의 추가 칸. 사진 칸과 같은 크기·점선이다. */
@Composable
fun LaimoryAddPhotoTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = "사진 추가",
) {
    Box(
        modifier =
            modifier
                .size(LaimoryPhotoTileDefaults.Size)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .photoTileBorder(MaterialTheme.colorScheme.outlineVariant)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ico_timeline_editor_add_photo),
            contentDescription = contentDescription,
            modifier = Modifier.size(ADD_ICON_SIZE),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 시안의 점선 테두리(1dp, 4·3 간격, 모서리 12). */
private fun Modifier.photoTileBorder(color: Color): Modifier =
    drawWithCache {
        val strokeWidth = 1.dp.toPx()
        val radius = 12.dp.toPx()
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
        onDrawBehind {
            drawRoundRect(
                color = color,
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = strokeWidth, pathEffect = pathEffect),
            )
        }
    }

private val REMOVE_BUTTON_SIZE = 22.dp
private val REMOVE_ICON_SIZE = 14.dp
private val ADD_ICON_SIZE = 20.dp

@Preview(name = "사진 칸 - 라이트", showBackground = true)
@Composable
private fun LaimoryPhotoTileLightPreview() {
    LaimoryTheme {
        PhotoTilePreviewRow()
    }
}

@Preview(name = "사진 칸 - 다크", showBackground = true, backgroundColor = 0xFF151311)
@Composable
private fun LaimoryPhotoTileDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        PhotoTilePreviewRow()
    }
}

@Composable
private fun PhotoTilePreviewRow() {
    Row(
        modifier = Modifier.padding(Spacing.large),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        LaimoryPhotoTile(onRemove = {}) {}
        LaimoryPhotoTile {}
        LaimoryAddPhotoTile(onClick = {})
    }
}
