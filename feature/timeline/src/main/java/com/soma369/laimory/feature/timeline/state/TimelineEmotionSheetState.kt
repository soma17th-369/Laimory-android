package com.soma369.laimory.feature.timeline.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.timeline.TimelineEmotion
import java.time.LocalDate

/**
 * 감정 선택 시트가 무엇을 위해 열렸는지.
 *
 * 서버 경로가 다르다 — 작성 완료는 `POST .../save` 로 DRAFT 를 SAVED 로 바꾸고, 수정은
 * `PUT .../emotion` 으로 감정만 갈아 끼운다. 시트를 하나로 두되 확인 버튼이 무엇을 부르는지는
 * 이 값이 정한다.
 */
enum class TimelineEmotionSheetPurpose {
    /** DRAFT 를 SAVED 로 확정하며 감정을 처음 정한다. */
    SAVE_RECORD,

    /** 이미 저장된 기록의 감정만 바꾼다. */
    EDIT_EMOTION,
}

@Immutable
data class TimelineEmotionSheetState(
    val recordDate: LocalDate,
    val dateLabel: String,
    val selected: TimelineEmotion = TimelineEmotion.DEFAULT_SELECTION,
    val purpose: TimelineEmotionSheetPurpose = TimelineEmotionSheetPurpose.SAVE_RECORD,
)
