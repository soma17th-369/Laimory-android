package com.soma369.laimory.feature.home.component

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/**
 * 3초마다 다음 항목으로 넘어가는 내용 슬롯. 일정·알림 카드가 같은 규칙을 쓴다.
 *
 * - 한 건뿐이면 돌리지 않는다. 0건이면 호출부가 빈 문구를 대신 그린다.
 * - **RESUMED 일 때만 돈다.** 백그라운드·다른 탭에서 돌면 돌아왔을 때 엉뚱한 항목이 떠 있고,
 *   보이지도 않는 화면을 위해 3초마다 깨어난다.
 * - 전환은 짧은 crossfade 다. 시스템 애니메이션 축소 설정은 Compose 가 `MotionDurationScale` 로
 *   이미 반영하므로 여기서 따로 읽지 않는다 — 배율 0 이면 즉시 바뀐다.
 * - **스크린 리더에 재낭독하지 않는다.** 3초마다 낭독이 끊기면 카드를 지나갈 수 없다. 라이브
 *   리전을 두지 않고, 카드 semantics 는 호출부가 한 문장으로 묶는다.
 */
@Composable
internal fun <T> HomeRotatingContent(
    items: List<T>,
    modifier: Modifier = Modifier,
    item: @Composable (slot: HomeRotatingSlot<T>) -> Unit,
) {
    // 자리가 아니라 **칸을** 넘긴다. Crossfade 는 사라지는 칸을 한 프레임 더 그리는데, 자리만
    // 넘기면 그 프레임이 이미 줄어든 새 목록을 그 자리로 뒤져 터진다.
    val slot = items.rotatingSlotAt(rememberRotatingIndex(items.size)) ?: return
    Crossfade(targetState = slot, modifier = modifier, label = "홈 원천 카드 회전") { current ->
        item(current)
    }
}

/** 현재 보여 줄 항목의 위치. [count] 가 바뀌면 처음으로 돌아간다. */
@Composable
private fun rememberRotatingIndex(count: Int): Int {
    var index by remember(count) { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(count, lifecycleOwner) {
        if (count <= 1) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                delay(ROTATION_INTERVAL_MILLIS)
                index = (index + 1) % count
            }
        }
    }
    return index
}

private const val ROTATION_INTERVAL_MILLIS = 3_000L
