package com.soma369.laimory.core.ui.component.coachmark

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.core.ui.theme.Spacing

/**
 * 화면을 어둡게 덮고 한 자리만 뚫어 무엇을 누르면 되는지 알려 주는 코치마크 한 단계.
 *
 * 화면 위에 따로 뜨는 창(Dialog)으로 그려 바텀바까지 함께 덮는다. 창이 모든 누름을 받으므로 **구멍 안의 원래 버튼은 눌리지
 * 않는다** — 짚어 주는 동안 다른 화면으로 새지 않는다. 어디를 누르든 다음 단계로 가고, `건너뛰기` 는 한 번에 끝낸다.
 * 뒤로가기도 끝내기와 같다.
 *
 * 짚을 자리가 스크롤 영역 밖에 있으면 먼저 끌어온다. 자리를 아직 잡지 못했으면(처음 그리는 중) 구멍 없이 말풍선만
 * 가운데에 띄운다. 자리의 크기가 바뀌면(사진 칸이 늦게 채워지는 등) 구멍이 따라간다.
 *
 * 말풍선은 구멍이 화면 위쪽에 있으면 그 아래, 아래쪽에 있으면 그 위에 둔다.
 *
 * @param targetKeys 짚을 자리. 여럿이면 모두를 덮는 사각형 하나로 뚫는다.
 * @param holeShape 구멍 모양. 둥근 버튼은 [CoachmarkHoleShape.CIRCLE] 로 뚫는다.
 * @param position 이 단계의 순번(1부터). [total] 과 함께 `n / N` 으로 보인다.
 */
@Composable
fun LaimoryCoachmark(
    state: CoachmarkState,
    targetKeys: List<String>,
    message: String,
    position: Int,
    total: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    holeShape: CoachmarkHoleShape = CoachmarkHoleShape.ROUNDED_RECT,
) {
    LaunchedEffect(targetKeys) { state.bringIntoView(targetKeys) }
    Dialog(
        onDismissRequest = onSkip,
        properties =
            DialogProperties(
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        // 창 기본 어둡게 하기는 구멍까지 덮는다. 끄고 직접 칠한다.
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }

        var origin by remember { mutableStateOf<Offset?>(null) }
        val hole =
            origin?.let { screenOrigin ->
                state.boundsOf(targetKeys)?.translate(-screenOrigin)?.inflate(HolePadding.value)
            }
        val scrim = MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { origin = it.positionOnScreen() }
                    .pointerInput(onNext) { detectTapGestures { onNext() } },
        ) {
            Canvas(
                modifier =
                    Modifier
                        .fillMaxSize()
                        // 구멍을 지우려면 따로 그린 판 위에서 지워야 한다. 그대로 지우면 아래 창까지 비친다.
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .clearAndSetSemantics { },
            ) {
                drawRect(scrim)
                hole?.let { rect ->
                    when (holeShape) {
                        CoachmarkHoleShape.ROUNDED_RECT -> {
                            val radius = HoleCornerRadius.toPx()
                            drawRoundRect(
                                color = Color.Black,
                                topLeft = rect.topLeft,
                                size = rect.size,
                                cornerRadius = CornerRadius(radius, radius),
                                blendMode = BlendMode.Clear,
                            )
                        }
                        CoachmarkHoleShape.CIRCLE ->
                            drawCircle(
                                color = Color.Black,
                                radius = rect.maxDimension / 2,
                                center = rect.center,
                                blendMode = BlendMode.Clear,
                            )
                    }
                }
            }
            CoachmarkBubbleLayout(hole = hole) {
                CoachmarkBubble(message = message, position = position, total = total, onNext = onNext, onSkip = onSkip)
            }
        }
    }
}

/** 말풍선을 구멍 아래 또는 위에 놓는다. 시스템 막대 안쪽으로 붙여 가려지지 않게 한다. */
@Composable
private fun CoachmarkBubbleLayout(
    hole: Rect?,
    content: @Composable () -> Unit,
) {
    val insets = WindowInsets.safeDrawing
    Layout(content = content, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val margin = BubbleMargin.roundToPx()
        val gap = BubbleGap.roundToPx()
        val topLimit = insets.getTop(this) + margin
        val bottomLimit = constraints.maxHeight - insets.getBottom(this) - margin
        val placeable =
            measurables.single().measure(
                constraints.copy(minWidth = 0, minHeight = 0, maxWidth = (constraints.maxWidth - margin * 2).coerceAtLeast(0)),
            )
        val preferredTop =
            when {
                hole == null -> (constraints.maxHeight - placeable.height) / 2
                hole.center.y < constraints.maxHeight / 2f -> hole.bottom.toInt() + gap
                else -> hole.top.toInt() - gap - placeable.height
            }
        val top = preferredTop.coerceIn(topLimit, (bottomLimit - placeable.height).coerceAtLeast(topLimit))
        layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(margin, top) }
    }
}

@Composable
private fun CoachmarkBubble(
    message: String,
    position: Int,
    total: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val isLast = position >= total
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = BubbleElevation,
    ) {
        Column(
            modifier = Modifier.padding(start = Spacing.large, end = Spacing.large, top = Spacing.large, bottom = Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$position / $total",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (!isLast) {
                    TextButton(onClick = onSkip) {
                        Text(
                            text = "건너뛰기",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Button(onClick = onNext) {
                    Text(text = if (isLast) "완료" else "다음", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private const val SCRIM_ALPHA = 0.64f
private val HolePadding = 6.dp
private val HoleCornerRadius = 16.dp
private val BubbleMargin = 16.dp
private val BubbleGap = 12.dp
private val BubbleElevation = 6.dp

@Preview(name = "코치마크 말풍선 - 라이트", showBackground = true)
@Composable
private fun CoachmarkBubbleLightPreview() {
    LaimoryTheme {
        Box(modifier = Modifier.padding(Spacing.large)) {
            CoachmarkBubble(message = "이 날 찍은 사진이에요. 눌러서 타임라인에 담을 사진을 골라 주세요.", position = 2, total = 5, onNext = {}, onSkip = {})
        }
    }
}

@Preview(name = "코치마크 말풍선 - 다크 · 마지막", showBackground = true, backgroundColor = 0xFF151311)
@Composable
private fun CoachmarkBubbleDarkPreview() {
    LaimoryTheme(darkTheme = true) {
        Box(modifier = Modifier.padding(Spacing.large)) {
            CoachmarkBubble(message = "다 골랐으면 여기를 눌러 타임라인을 만들어 보세요.", position = 5, total = 5, onNext = {}, onSkip = {})
        }
    }
}
