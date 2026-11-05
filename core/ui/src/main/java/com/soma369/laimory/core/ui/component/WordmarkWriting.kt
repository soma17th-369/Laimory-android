package com.soma369.laimory.core.ui.component

import android.animation.ValueAnimator
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionResult
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.soma369.laimory.core.ui.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 워드마크가 한 획으로 써졌다 지워지기를 되풀이한다. 기다리는 동안 칸을 채우는 로딩 표시다.
 *
 * 그림은 워드마크와 같은 이유로 브랜드 색 고정이다. 장식이라 접근성 설명은 두지 않는다 — 무엇을 기다리는지는 화면 문구가 말한다.
 *
 * 기기에서 애니메이션을 꺼 두었으면 다 써진 로고 한 장으로 멈춘다. 반복의 마지막 프레임은 빈 화면이라, 시스템 배율 0 에
 * 맡기면 아무것도 보이지 않는다.
 */
@Composable
fun WordmarkWritingLoop(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_wordmark_writing))
    if (!rememberAnimationsEnabled()) {
        LottieAnimation(composition = composition, progress = { composition?.introEndProgress() ?: 0f }, modifier = modifier)
        return
    }
    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)
    LottieAnimation(composition = composition, progress = { progress }, modifier = modifier)
}

/**
 * 워드마크를 한 번 써 보이고 i 의 점이 자리 잡으면 [onFinished] 를 부른다. 앱 시작 스플래시가 쓴다.
 *
 * 어떤 경우에도 [onFinished] 는 한 번 불린다 — 애니메이션을 읽지 못하면 바로, 기기에서 꺼 두었으면 기다리지 않고 부른다.
 * 스플래시가 이 끝을 기다리므로 여기서 멈추면 앱이 가려진 채로 남는다.
 *
 * 처음 그린 뒤 [startDeadlineMillis] 안에 시작하지 못하면 쓰지 않고 끝낸다. 앱 시작 직후는 아래 화면을 만드는 일과 메인
 * 스레드를 나눠 쓰는데, 느린 기기나 디버그 빌드에서는 몇 초씩 밀린다. 그동안 빈 화면으로 붙잡느니 바로 앱을 보여 준다.
 */
@Composable
fun WordmarkWritingIntro(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    speed: Float = 1f,
    startDeadlineMillis: Long = Long.MAX_VALUE,
) {
    val shownAt = remember { SystemClock.uptimeMillis() }
    val result = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_wordmark_writing))
    val composition by result
    val animatable = rememberLottieAnimatable()
    val currentOnFinished by rememberUpdatedState(onFinished)
    val animationsEnabled = rememberAnimationsEnabled()
    LaunchedEffect(Unit) {
        if (!animationsEnabled) return@LaunchedEffect currentOnFinished()
        // 기한은 읽기를 기다리는 동안에도 센다. 읽기가 끝난 뒤에만 재면 느린 빌드에서 읽는 몇 초 내내 빈 화면이 남는다.
        val remaining = startDeadlineMillis - (SystemClock.uptimeMillis() - shownAt)
        val loaded = result.awaitOrNull(remaining)
        if (loaded == null || SystemClock.uptimeMillis() - shownAt > startDeadlineMillis) {
            return@LaunchedEffect currentOnFinished()
        }
        animatable.animate(loaded, clipSpec = LottieClipSpec.Marker(MARKER_INTRO), speed = speed)
        currentOnFinished()
    }
    LottieAnimation(composition = composition, progress = { animatable.progress }, modifier = modifier)
}

/**
 * 워드마크 애니메이션을 미리 읽기 시작한다. 스플래시를 띄우기로 정한 즉시 불러, 화면을 만드는 동안 읽기가 함께 진행되게 한다.
 * 같은 리소스는 Lottie 가 한 번만 읽고 결과를 나눠 준다.
 */
fun preloadWordmarkWriting(context: Context) {
    LottieCompositionFactory.fromRawRes(context, R.raw.lottie_wordmark_writing)
}

@Composable
private fun rememberAnimationsEnabled(): Boolean = remember { ValueAnimator.areAnimatorsEnabled() }

/**
 * [timeoutMillis] 안에 읽히면 그 결과를, 시간이 지나거나 읽기에 실패하면 null 을 준다.
 * await 는 읽기 실패 원인을 그대로 던지는데, 어떤 실패든 호출부에는 '못 그림' 하나라 가리지 않는다.
 */
private suspend fun LottieCompositionResult.awaitOrNull(timeoutMillis: Long): LottieComposition? =
    try {
        withTimeoutOrNull(timeoutMillis.coerceAtLeast(0L)) { await() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

/** 다 써지고 점이 자리 잡은 프레임의 진행률. */
private fun LottieComposition.introEndProgress(): Float {
    val intro = getMarker(MARKER_INTRO) ?: return 0f
    return getProgressForFrame(intro.startFrame + intro.durationFrames)
}

/** 생성기(tools/lottie/wordmark_writing.py)가 넣는 구간 이름. */
private const val MARKER_INTRO = "intro"
