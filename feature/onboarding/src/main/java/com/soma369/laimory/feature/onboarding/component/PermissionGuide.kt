package com.soma369.laimory.feature.onboarding.component

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.soma369.laimory.core.ui.theme.Spacing
import com.soma369.laimory.feature.onboarding.model.PermissionGuideSpec
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.unit.lerp as lerpDp

/**
 * 시스템 권한 창에서 무엇을 눌러야 하는지 미리 보여 주는 안내.
 *
 * 시스템 창은 앱이 꾸밀 수 없고 **그 위에 겹쳐 그릴 수도 없다** — 권한 창은 다른 앱이 위에 그린
 * 것을 숨긴다(탭재킹 방지). 그래서 우리 화면의 그림 자리에서 창을 흉내 낸 모형으로 보여 준다.
 * 모형은 눌리지 않는다.
 *
 * 한 바퀴 3.45초를 되풀이한다: 대기 → 다가가기 → 누름 → 반짝 → 유지 → 처음으로.
 * 기기에서 애니메이션을 꺼 둔 사용자에게는 **반짝인 상태로 멈춰** 무엇을 누를지만 보여 준다 —
 * 멀미나 주의력 때문에 끈 설정을 이 화면만 무시할 이유가 없다.
 */
@Composable
internal fun PermissionGuide(
    spec: PermissionGuideSpec,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isAnimationEnabled =
        remember(context) {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }
    val transition = rememberInfiniteTransition(label = "permission-guide")

    // 한 바퀴가 제자리에서 끝나야 이어 붙였을 때 튀지 않는다 — 시작값과 끝값을 같게 둔다.
    val approachAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = CYCLE_MILLIS
                        0f at WAIT_MILLIS using FastOutSlowInEasing
                        1f at APPROACH_END
                        1f at HOLD_END using FastOutSlowInEasing
                    },
            ),
        label = "approach",
    )
    val fingerScaleAnimation by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = CYCLE_MILLIS
                        1f at APPROACH_END using LinearOutSlowInEasing
                        PRESSED_FINGER_SCALE at PRESS_END using LinearOutSlowInEasing
                        1f at FLASH_END
                    },
            ),
        label = "finger-scale",
    )
    val highlightAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = CYCLE_MILLIS
                        0f at APPROACH_END using LinearOutSlowInEasing
                        1f at PRESS_END
                        1f at HOLD_END using FastOutSlowInEasing
                    },
            ),
        label = "highlight",
    )
    val glowAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = CYCLE_MILLIS
                        0f at PRESS_END using LinearOutSlowInEasing
                        1f at FLASH_END
                        1f at HOLD_END using FastOutSlowInEasing
                    },
            ),
        label = "glow",
    )
    val rippleScaleAnimation by transition.animateFloat(
        initialValue = RIPPLE_PRESS_SCALE,
        targetValue = RIPPLE_PRESS_SCALE,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = CYCLE_MILLIS
                        RIPPLE_PRESS_SCALE at APPROACH_END using LinearOutSlowInEasing
                        1f at PRESS_END using LinearOutSlowInEasing
                        RIPPLE_SPREAD_SCALE at FLASH_END
                    },
            ),
        label = "ripple-scale",
    )
    val rippleAlphaAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    keyframes {
                        durationMillis = CYCLE_MILLIS
                        0f at APPROACH_END
                        RIPPLE_ALPHA at PRESS_END using LinearOutSlowInEasing
                        0f at FLASH_END
                    },
            ),
        label = "ripple-alpha",
    )

    // 멈춘 화면은 마지막 단계(반짝)다. 무엇을 눌렀는지가 남아 있어야 안내가 된다.
    val approach = if (isAnimationEnabled) approachAnimation else 1f
    val fingerScale = if (isAnimationEnabled) fingerScaleAnimation else 1f
    val highlight = if (isAnimationEnabled) highlightAnimation else 1f
    val glow = if (isAnimationEnabled) glowAnimation else 1f
    val rippleAlpha = if (isAnimationEnabled) rippleAlphaAnimation else 0f

    var root by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var highlightedBounds by remember { mutableStateOf<Rect?>(null) }

    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = GUIDE_MIN_HEIGHT).onGloballyPositioned { root = it },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            GuideCaption(text = spec.caption)
            GuideDialog(
                spec = spec,
                highlight = highlight,
                glow = glow,
                onHighlightPositioned = { coordinates ->
                    highlightedBounds = root?.localBoundingBoxOf(coordinates)
                },
            )
        }

        val bounds = highlightedBounds
        val size = root?.size
        if (bounds != null && size != null) {
            // 누르는 자리는 강조된 선택지의 오른쪽이다. 가운데를 누르면 손가락이 글자를 가린다.
            val touch = Offset(x = bounds.right - TOUCH_INSET_PX, y = bounds.center.y)
            val rest = Offset(x = size.width - REST_INSET_X_PX, y = size.height - REST_INSET_Y_PX)
            val point = lerp(rest, touch, approach)

            if (rippleAlpha > 0f) {
                GuideRipple(
                    center = touch,
                    scale = rippleScaleAnimation,
                    alpha = rippleAlpha,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            GuideFinger(point = point, scale = fingerScale)
        }
    }
}

/** 창이 뜨기 전에 무엇을 누를지 말하는 줄. 그림과 달리 이것은 읽어 준다. */
@Composable
private fun GuideCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.secondaryContainer, CAPTION_SHAPE)
                .padding(horizontal = Spacing.medium, vertical = CAPTION_VERTICAL_PADDING),
    )
}

/** 시스템 창 모형. 실제 창과 헷갈리지 않게 화면보다 작게 그린다. */
@Composable
private fun GuideDialog(
    spec: PermissionGuideSpec,
    highlight: Float,
    glow: Float,
    onHighlightPositioned: (LayoutCoordinates) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .width(DIALOG_WIDTH)
                .shadow(DIALOG_ELEVATION, DIALOG_SHAPE)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, DIALOG_SHAPE)
                .padding(horizontal = Spacing.large, vertical = Spacing.extraLarge)
                // 모형은 장식이다. 같은 안내를 캡션이 글자로 말하므로 여기까지 읽으면 소음이 된다.
                .clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        Text(
            text = spec.title,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.small)) {
            spec.options.forEachIndexed { index, option ->
                val isHighlighted = index == spec.highlightedIndex
                GuideOption(
                    text = option,
                    highlight = if (isHighlighted) highlight else 0f,
                    glow = if (isHighlighted) glow else 0f,
                    modifier =
                        if (isHighlighted) {
                            Modifier.onGloballyPositioned(onHighlightPositioned)
                        } else {
                            Modifier
                        },
                )
            }
        }
    }
}

/** 창의 선택지 한 줄. 눌러야 하는 줄만 강조가 올라온다. */
@Composable
private fun GuideOption(
    text: String,
    highlight: Float,
    glow: Float,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = OPTION_MIN_HEIGHT)
                .shadow(
                    elevation = lerpDp(0.dp, OPTION_GLOW_ELEVATION, glow),
                    shape = OPTION_SHAPE,
                    ambientColor = scheme.primary,
                    spotColor = scheme.primary,
                )
                .background(lerpColor(scheme.surface, scheme.primaryContainer, highlight), OPTION_SHAPE)
                .border(
                    width = lerpDp(OPTION_BORDER_WIDTH, OPTION_HIGHLIGHT_BORDER_WIDTH, highlight),
                    color = lerpColor(scheme.outlineVariant, scheme.primary, highlight),
                    shape = OPTION_SHAPE,
                )
                .padding(horizontal = Spacing.large, vertical = Spacing.small),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = lerpColor(scheme.onSurface, scheme.onPrimaryContainer, highlight),
            textAlign = TextAlign.Center,
        )
    }
}

/** 누른 자리에서 퍼지는 원. */
@Composable
private fun BoxScope.GuideRipple(
    center: Offset,
    scale: Float,
    alpha: Float,
    color: Color,
) {
    Canvas(modifier = Modifier.matchParentSize().clearAndSetSemantics { }) {
        drawCircle(color = color, radius = RIPPLE_RADIUS.toPx() * scale, center = center, alpha = alpha)
    }
}

/** 누르는 손가락. 에셋 경로를 그대로 쓰고 색만 테마를 따른다. */
@Composable
private fun BoxScope.GuideFinger(
    point: Offset,
    scale: Float,
) {
    val fill = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.surface
    val finger =
        remember(fill, outline) {
            ImageVector.Builder(
                defaultWidth = FINGER_SIZE,
                defaultHeight = FINGER_SIZE,
                viewportWidth = FINGER_VIEWPORT,
                viewportHeight = FINGER_VIEWPORT,
            ).addPath(
                pathData = PathParser().parsePathString(FINGER_PATH).toNodes(),
                fill = SolidColor(fill),
                stroke = SolidColor(outline),
                strokeLineWidth = FINGER_STROKE_WIDTH,
            ).build()
        }

    Image(
        imageVector = finger,
        contentDescription = null,
        modifier =
            Modifier
                .size(FINGER_SIZE)
                .offset {
                    IntOffset(
                        // 손끝이 누르는 자리에 오도록 그림을 끌어 올린다. 그림의 왼쪽 위가 아니라
                        // 손끝이 기준점이다.
                        x = (point.x - FINGER_SIZE.toPx() * FINGER_TIP_X).roundToInt(),
                        y = (point.y - FINGER_SIZE.toPx() * FINGER_TIP_Y).roundToInt(),
                    )
                }
                .scale(scale)
                .clearAndSetSemantics { },
    )
}

/** 한 바퀴. 시안의 명세(대기 0.6 → 다가가기 0.6 → 누름 0.15 → 반짝 0.5 → 유지 1.2 → 복귀 0.4)다. */
private const val WAIT_MILLIS = 600
private const val APPROACH_MILLIS = 600
private const val PRESS_MILLIS = 150
private const val FLASH_MILLIS = 500
private const val HOLD_MILLIS = 1200
private const val RETURN_MILLIS = 400

private const val APPROACH_END = WAIT_MILLIS + APPROACH_MILLIS
private const val PRESS_END = APPROACH_END + PRESS_MILLIS
private const val FLASH_END = PRESS_END + FLASH_MILLIS
private const val HOLD_END = FLASH_END + HOLD_MILLIS
private const val CYCLE_MILLIS = HOLD_END + RETURN_MILLIS

/** 누를 때 손가락이 줄어드는 정도. */
private const val PRESSED_FINGER_SCALE = 0.9f

/** 퍼짐 원: 누를 때 24%로 나타나 2.4배로 커지며 사라진다. */
private const val RIPPLE_ALPHA = 0.24f
private const val RIPPLE_PRESS_SCALE = 0.4f
private const val RIPPLE_SPREAD_SCALE = 2.4f
private val RIPPLE_RADIUS = 20.dp

/** 손끝의 자리. 그림 상자 안에서 손톱 끝이 오는 지점의 비율이다. */
private const val FINGER_TIP_X = 0.45f
private const val FINGER_TIP_Y = 0.3f

private val FINGER_SIZE = 40.dp
private const val FINGER_VIEWPORT = 40f
private const val FINGER_STROKE_WIDTH = 1f

/** 시안에서 내려받은 손가락 에셋의 경로. 모양을 새로 그리지 않는다. */
private const val FINGER_PATH =
    "M19.1667 9.5C20.8261 9.5 22.1667 10.8405 22.1667 12.5V22H22.9333C23.2246 22 23.5024 22.0463 23.7653 22.1279" +
        "L24.0241 22.2207L24.0397 22.2275L24.0563 22.2354L31.6227 26.002H31.6217C32.6735 26.453 33.4167 27.524 " +
        "33.4167 28.75C33.4167 28.8928 33.3938 29.0478 33.3786 29.1543L32.1286 37.9375L32.1276 37.9414C31.9107 " +
        "39.3801 30.7524 40.5 29.2331 40.5H17.9167C17.095 40.5 16.3362 40.1598 15.7965 39.6201L7.21159 31.0352" +
        "L7.5612 30.6816L8.8776 29.3486L8.87956 29.3467C9.29975 28.9265 9.89172 28.6504 10.5505 28.6504C10.663 " +
        "28.6504 10.7589 28.6624 10.8356 28.6748L11.0124 28.7041L11.0329 28.7061L11.0524 28.7109L16.1667 29.7842" +
        "V12.5C16.1667 10.8405 17.5072 9.5 19.1667 9.5ZM19.1667 4.5C23.5928 4.5 27.1667 8.07386 27.1667 12.5C" +
        "27.1667 15.2738 25.7604 17.7096 23.611 19.1484L22.8337 19.6699V12.5C22.8337 10.4761 21.1905 8.83301 " +
        "19.1667 8.83301C17.1428 8.83301 15.4997 10.4761 15.4997 12.5V19.6699L14.7223 19.1484C12.5729 17.7096 " +
        "11.1667 15.2738 11.1667 12.5C11.1667 8.07386 14.7405 4.5 19.1667 4.5Z"

/** 쉬는 자리. 시안이 그림 오른쪽 아래에 손가락을 둔다. */
private const val REST_INSET_X_PX = 70f
private const val REST_INSET_Y_PX = 46f

/** 강조된 선택지에서 누르는 지점(오른쪽 끝에서 안쪽으로). */
private const val TOUCH_INSET_PX = 48f

/** 시안 값. 창은 272 폭에 모서리 24, 선택지는 높이 40에 모서리 20이다. */
private val DIALOG_WIDTH = 272.dp
private val DIALOG_SHAPE = RoundedCornerShape(24.dp)
private val DIALOG_ELEVATION = 2.dp
private val OPTION_SHAPE = RoundedCornerShape(20.dp)
private val OPTION_MIN_HEIGHT = 40.dp
private val OPTION_BORDER_WIDTH = 1.dp
private val OPTION_HIGHLIGHT_BORDER_WIDTH = 2.dp
private val OPTION_GLOW_ELEVATION = 12.dp
private val CAPTION_SHAPE = RoundedCornerShape(percent = 50)
private val CAPTION_VERTICAL_PADDING = 6.dp

/** 그림 자리의 최소 높이. 시안의 image-wrap 이 300 이다. */
private val GUIDE_MIN_HEIGHT = 300.dp
