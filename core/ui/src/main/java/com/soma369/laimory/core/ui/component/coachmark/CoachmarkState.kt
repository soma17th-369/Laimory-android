package com.soma369.laimory.core.ui.component.coachmark

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.positionOnScreen

/**
 * 코치마크가 짚을 자리들. 화면이 [coachmarkTarget] 으로 자리마다 이름을 붙여 두면 [LaimoryCoachmark] 가 그 이름으로 구멍을 뚫는다.
 *
 * 자리는 **화면 좌표**로 든다. 코치마크는 화면 위에 따로 뜨는 창이라 같은 창 좌표를 쓸 수 없다. 스크롤로 일부가 가려진 자리는
 * 보이는 부분만 든다 — 가려진 곳까지 뚫으면 다른 요소 위에 구멍이 난다.
 */
@Stable
class CoachmarkState internal constructor() {
    private val bounds = mutableStateMapOf<String, Rect>()
    private val requesters = mutableMapOf<String, BringIntoViewRequester>()

    /** [keys] 를 모두 덮는 사각형. 아직 자리를 잡지 못한 이름이 있으면 null 이다. */
    internal fun boundsOf(keys: List<String>): Rect? {
        val rects = keys.map { bounds[it] ?: return null }
        if (rects.isEmpty()) return null
        return Rect(
            left = rects.minOf(Rect::left),
            top = rects.minOf(Rect::top),
            right = rects.maxOf(Rect::right),
            bottom = rects.maxOf(Rect::bottom),
        )
    }

    /** 스크롤 영역 안에서 화면 밖에 있는 자리를 끌어온다. */
    internal suspend fun bringIntoView(keys: List<String>) {
        keys.forEach { requesters[it]?.bringIntoView() }
    }

    internal fun register(
        key: String,
        requester: BringIntoViewRequester,
    ) {
        requesters[key] = requester
    }

    internal fun unregister(key: String) {
        requesters.remove(key)
        bounds.remove(key)
    }

    internal fun update(
        key: String,
        coordinates: LayoutCoordinates,
    ) {
        if (!coordinates.isAttached) return
        // 창 안에서 잘린 경계를 화면 좌표로 옮긴다.
        val windowToScreen = coordinates.positionOnScreen() - coordinates.positionInWindow()
        bounds[key] = coordinates.boundsInWindow().translate(windowToScreen)
    }
}

@Composable
fun rememberCoachmarkState(): CoachmarkState = remember { CoachmarkState() }

/** 이 요소를 코치마크가 [key] 로 짚을 수 있게 한다. 같은 이름을 여러 요소에 붙이지 않는다. */
fun Modifier.coachmarkTarget(
    state: CoachmarkState,
    key: String,
): Modifier =
    composed {
        val requester = remember { BringIntoViewRequester() }
        DisposableEffect(state, key) {
            state.register(key, requester)
            onDispose { state.unregister(key) }
        }
        Modifier
            .bringIntoViewRequester(requester)
            .onGloballyPositioned { state.update(key, it) }
    }
