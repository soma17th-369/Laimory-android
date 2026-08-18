package com.soma369.laimory.feature.timeline.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import java.time.LocalDate

/**
 * 하루 감정 선택 시트 상태.
 *
 * 시트를 여는 순간 [selected]가 채워지므로 "고르지 않은 상태"는 존재하지 않는다 — 서버가 감정을 필수로
 * 받기 때문에 미선택 저장 경로를 아예 만들지 않는다.
 *
 * [dateLabel]은 안내 문구에 들어갈 날짜 표현(`오늘`·`어제`·`05.14`)이다. 기준이 되는 오늘은 주입된
 * 시계로 정해야 해서 화면이 아니라 ViewModel이 만든다.
 */
@Immutable
data class TimelineEmotionSheetState(
    val recordDate: LocalDate,
    val dateLabel: String,
    val selected: TimelineEmotion = TimelineEmotion.DEFAULT_SELECTION,
)
