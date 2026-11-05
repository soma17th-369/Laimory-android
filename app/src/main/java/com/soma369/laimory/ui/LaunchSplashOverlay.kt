package com.soma369.laimory.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.soma369.laimory.R
import com.soma369.laimory.core.ui.component.WordmarkWritingIntro
import kotlinx.coroutines.delay

/**
 * 앱을 새로 켤 때 첫 화면을 덮고 워드마크를 한 번 써 보이는 스플래시.
 *
 * 시스템 스플래시(Android 12+ 는 건너뛸 수 없다) 안에서는 Lottie 를 못 돌린다. 그래서 시스템 스플래시는 같은 배경색만
 * 보여 주고(`Theme.Laimory.Starting`), 이어서 이 화면이 같은 자리에서 워드마크를 쓴다. 다 쓰고 잠깐 멈췄다가 흐려지며
 * 사라지고, **다 사라진 뒤에** [onFinished] 를 부른다. 호출부는 그때 기다리게 했던 창(공지 시트 등)을 풀어 준다.
 *
 * 배경은 앱 테마가 아니라 시스템 스플래시와 같은 색(OS 다크 설정을 따르는 `splash_background`)이다. 앱에서 화면 모드를
 * 따로 골랐으면 둘이 다른데, 시스템 스플래시에서 이어질 때 색이 끊기지 않고 사라질 때 흐려지며 앱 색으로 넘어간다.
 *
 * 덮는 동안 아래 화면의 입력과 back 을 막는다. 아래 화면은 그대로 만들어지고 있어 손이 닿으면 보이지 않는 버튼이 눌린다.
 */
@Composable
internal fun LaunchSplashOverlay(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleState = remember { MutableTransitionState(true) }
    val currentOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
        if (visibleState.isIdle && !visibleState.currentState) currentOnFinished()
    }
    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        exit = fadeOut(tween(EXIT_FADE_MILLIS)),
    ) {
        BackHandler { }
        var isWritten by remember { mutableStateOf(false) }
        LaunchedEffect(isWritten) {
            if (!isWritten) return@LaunchedEffect
            delay(HOLD_AFTER_WRITTEN_MILLIS)
            visibleState.targetState = false
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(colorResource(R.color.splash_background))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            contentAlignment = Alignment.Center,
        ) {
            WordmarkWritingIntro(
                onFinished = { isWritten = true },
                modifier = Modifier.width(WordmarkWidth),
                speed = WRITING_SPEED,
                startDeadlineMillis = START_DEADLINE_MILLIS,
            )
        }
    }
}

private val WordmarkWidth = 200.dp

/** 써지기 구간(약 1.5초)을 조금 빨리 돌려 앱 시작을 오래 붙잡지 않는다. */
private const val WRITING_SPEED = 1.25f
private const val HOLD_AFTER_WRITTEN_MILLIS = 150L

/** release 실측으로 첫 그림부터 시작까지 50ms 안팎이다. 이보다 크게 밀리면 기기가 바쁜 것이라 건너뛴다. */
private const val START_DEADLINE_MILLIS = 600L
private const val EXIT_FADE_MILLIS = 250
