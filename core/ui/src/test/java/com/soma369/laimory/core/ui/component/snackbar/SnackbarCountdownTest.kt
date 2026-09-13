package com.soma369.laimory.core.ui.component.snackbar

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnackbarCountdownTest {
    @Test
    fun `애니메이션 배율이 0이어도 실제 시간만큼 센다`() {
        // 접근성의 애니메이션 제거는 배율을 0 으로 만든다. 애니메이션 API 로 세면 첫 프레임에 끝나 곧바로 닫혔다.
        val clock = FakeFrameClock(frameMillis = 16L)

        runBlocking { withContext(clock + NoMotion) { countDownByFrames(totalMillis = 5_000L) {} } }

        assertTrue(clock.nowMillis - clock.firstFrameMillis >= 5_000L)
    }

    @Test
    fun `남은 비율은 1에서 줄어 끝에서 0이 된다`() {
        val clock = FakeFrameClock(frameMillis = 1_000L)
        val reported = mutableListOf<Float>()

        runBlocking { withContext(clock) { countDownByFrames(totalMillis = 4_000L) { reported += it } } }

        assertEquals(listOf(0.75f, 0.5f, 0.25f, 0f), reported)
    }

    private object NoMotion : MotionDurationScale {
        override val scaleFactor: Float = 0f
    }

    /** 요청마다 한 프레임씩 시각을 앞으로 민다. 실제로 기다리지 않는다. */
    private class FakeFrameClock(
        private val frameMillis: Long,
    ) : MonotonicFrameClock {
        var nowMillis = 0L
            private set
        var firstFrameMillis = -1L
            private set

        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            nowMillis += frameMillis
            if (firstFrameMillis < 0L) firstFrameMillis = nowMillis
            return onFrame(nowMillis * 1_000_000L)
        }
    }
}
