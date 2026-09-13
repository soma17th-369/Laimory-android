package com.soma369.laimory.core.ui.component.snackbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/**
 * 앱 공용 스낵바 호스트.
 *
 * 평소 스낵바는 Material3 기본 모양 그대로 그린다. [TimedSnackbarVisuals] 로 온 것만 위쪽 가장자리에
 * 남은 시간 막대를 얹고, 막대가 다 줄면 닫는다.
 */
@Composable
fun LaimorySnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        when (val visuals = data.visuals) {
            is TimedSnackbarVisuals -> TimedSnackbar(data = data, visuals = visuals)
            else -> Snackbar(data)
        }
    }
}

@Composable
private fun TimedSnackbar(
    data: SnackbarData,
    visuals: TimedSnackbarVisuals,
) {
    val accessibilityManager = LocalAccessibilityManager.current
    // TalkBack 사용자는 action 에 닿기까지 더 걸린다. Material3 가 Short·Long 에 하는 것과 같게 늘린다.
    val totalMillis =
        remember(visuals, accessibilityManager) {
            accessibilityManager?.calculateRecommendedTimeoutMillis(
                visuals.durationMillis,
                false,
                true,
                visuals.actionLabel != null,
            ) ?: visuals.durationMillis
        }
    var remaining by remember(data) { mutableFloatStateOf(1f) }
    LaunchedEffect(data) {
        countDownByFrames(totalMillis) { remaining = it }
        // 막대가 끝나는 순간이 곧 닫히는 순간이다. 타이머를 따로 두면 막대가 남았는데 닫히거나 그 반대가 된다.
        data.dismiss()
    }
    Box(
        modifier =
            Modifier
                // 기본 스낵바와 같은 바깥 여백이다. 막대를 모서리 안에 가두려고 여백 안쪽에서 자른다.
                .padding(SnackbarOuterPadding)
                .clip(SnackbarDefaults.shape),
    ) {
        Snackbar(
            action =
                visuals.actionLabel?.let { label ->
                    {
                        TextButton(
                            onClick = data::performAction,
                            colors = ButtonDefaults.textButtonColors(contentColor = SnackbarDefaults.actionColor),
                        ) {
                            Text(label)
                        }
                    }
                },
        ) {
            Text(visuals.message)
        }
        LinearProgressIndicator(
            progress = { remaining },
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(RemainingBarHeight)
                    // 남은 시간은 보는 사람을 위한 표시다. 낭독하면 문구와 action 사이에 진행률이 끼어든다.
                    .clearAndSetSemantics { },
            color = SnackbarDefaults.actionColor,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

/**
 * 스낵바 수명을 **실제 경과 시간**으로 센다. 프레임마다 남은 비율(1 → 0)을 알리고, 시간이 다 되면 돌아온다.
 *
 * `Animatable`·`tween` 을 쓰지 않는다. 애니메이션 API 는 기기 애니메이션 배율(`MotionDurationScale`)을
 * 따르므로, 접근성의 애니메이션 제거나 개발자 옵션으로 배율이 0 이면 첫 프레임에 끝나 곧바로 닫힌다 —
 * 문구를 읽거나 action 을 누를 시간이 없다. 프레임 시각은 배율과 무관하다.
 */
internal suspend fun countDownByFrames(
    totalMillis: Long,
    onRemaining: (Float) -> Unit,
) {
    val startMillis = withFrameMillis { it }
    while (true) {
        val elapsedMillis = withFrameMillis { it } - startMillis
        onRemaining(remainingFraction(elapsedMillis, totalMillis))
        if (elapsedMillis >= totalMillis) return
    }
}

private fun remainingFraction(
    elapsedMillis: Long,
    totalMillis: Long,
): Float = if (totalMillis <= 0L) 0f else (1f - elapsedMillis.toFloat() / totalMillis).coerceIn(0f, 1f)

/** Material3 `Snackbar(snackbarData)` 가 두르는 바깥 여백과 같다. */
private val SnackbarOuterPadding = 12.dp
private val RemainingBarHeight = 3.dp
