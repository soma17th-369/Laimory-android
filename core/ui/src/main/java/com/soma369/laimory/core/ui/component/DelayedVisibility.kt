package com.soma369.laimory.core.ui.component

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * 짧은 기다림에는 로딩을 보이지 않는다. [active] 가 [delayMillis] 넘게 이어질 때만 참이 되고, 한번 참이 되면 [active] 가
 * 거짓이 돼도 [minVisibleMillis] 를 채운 뒤에 거짓이 된다.
 *
 * 0.1초 안팎으로 끝나는 조회에 로딩을 그대로 걸면 한두 프레임만 번쩍여 버그처럼 보인다. 늦을 때만 띄우고, 띄우면 잠깐
 * 유지해 깜빡이 아니라 상태로 읽히게 한다. 기다리는 동안 그 자리는 비워 두되 크기는 호출부가 지킨다.
 */
@Composable
fun rememberDelayedVisibility(
    active: Boolean,
    delayMillis: Long = LOADING_SHOW_DELAY_MILLIS,
    minVisibleMillis: Long = LOADING_MIN_VISIBLE_MILLIS,
): Boolean {
    var visible by remember { mutableStateOf(false) }
    var shownAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(active) {
        if (active) {
            if (!visible) {
                delay(delayMillis)
                visible = true
                shownAt = SystemClock.uptimeMillis()
            }
        } else if (visible) {
            val left = minVisibleMillis - (SystemClock.uptimeMillis() - shownAt)
            if (left > 0) delay(left)
            visible = false
        }
    }
    return visible
}

private const val LOADING_SHOW_DELAY_MILLIS = 300L
private const val LOADING_MIN_VISIBLE_MILLIS = 500L
