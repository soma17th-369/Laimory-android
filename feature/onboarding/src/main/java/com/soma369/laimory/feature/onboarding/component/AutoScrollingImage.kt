package com.soma369.laimory.feature.onboarding.component

import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import com.soma369.laimory.core.ui.theme.LaimoryShapes
import kotlinx.coroutines.delay

/**
 * 화면보다 긴 그림을 정해진 창 안에서 위에서 아래로 흘려 보여 준다.
 *
 * 끝에 닿으면 잠깐 멈췄다가 **맨 위로 돌아가 다시 내려온다.** 되감기를 애니메이션으로 보여 주면
 * 타임라인이 거꾸로 흐르는 것처럼 읽혀, 예시가 아니라 오작동으로 보인다.
 *
 * 기기에서 애니메이션을 꺼 둔 사용자에게는 흐르지 않고 첫 화면만 보여 준다 — 멀미나 주의력
 * 문제로 끈 설정을 이 화면만 무시할 이유가 없다.
 */
@Composable
internal fun AutoScrollingImage(
    @DrawableRes image: Int,
    viewportHeight: Dp,
    /** 창 안에서 그림이 차지하는 폭. 시안이 창보다 좁게 두고 가운데 정렬한다. */
    imageWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isAnimationEnabled =
        remember(context) {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState.maxValue, isAnimationEnabled) {
        if (!isAnimationEnabled || scrollState.maxValue == 0) return@LaunchedEffect
        // 첫 화면을 잠깐 보여 준 뒤 흐르기 시작한다. 뜨자마자 움직이면 무엇이 흐르는지 못 읽는다.
        delay(START_DELAY_MILLIS)
        while (true) {
            scrollState.animateScrollTo(
                value = scrollState.maxValue,
                animationSpec = tween(durationMillis = SCROLL_DURATION_MILLIS, easing = LinearEasing),
            )
            delay(END_PAUSE_MILLIS)
            scrollState.scrollTo(0)
            delay(START_DELAY_MILLIS)
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(viewportHeight)
                .clip(LaimoryShapes.medium)
                // 장식이라 읽을 것이 없다. 스크롤 상태까지 접근성 트리에 노출되면 소음이 된다.
                .clearAndSetSemantics { }
                .verticalScroll(scrollState, enabled = false),
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = null,
            modifier = Modifier.width(imageWidth).align(Alignment.TopCenter),
            contentScale = ContentScale.FillWidth,
        )
    }
}

/** 흐르기 전후로 두는 정지. 끝에 닿았다는 것을 알아볼 만큼만 둔다. */
private const val END_PAUSE_MILLIS = 600L
private const val START_DELAY_MILLIS = 400L

/** 위에서 아래까지 걸리는 시간. 글을 읽을 수 있을 만큼 느리게 둔다. */
private const val SCROLL_DURATION_MILLIS = 9000
