package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import java.time.Instant

/**
 * 일정 카드가 3초마다 한 건씩 넘기며 보여 주는 일정.
 *
 * 카드가 쓰는 것은 시각과 제목뿐이라 그 둘만 담는다. 순서는 시작 시각 오름차순이고 같은 시각이면
 * `rawId` 오름차순이다 — 회전이 안정적이려면 정렬이 흔들리지 않아야 한다.
 */
@Immutable
data class HomeCalendarItem(
    val rawId: String,
    val title: String,
    val startAt: Instant,
    val endAt: Instant?,
    val allDay: Boolean,
)
