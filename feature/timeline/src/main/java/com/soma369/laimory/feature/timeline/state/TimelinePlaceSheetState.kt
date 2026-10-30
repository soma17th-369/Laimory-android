package com.soma369.laimory.feature.timeline.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.domain.usecase.UpdateTimelineEventPlaceUseCase

/**
 * 열려 있는 장소 고르기 시트.
 *
 * 목록과 직접 입력 두 단계가 한 시트 안에서 오간다. 직접 입력에서 뒤로 오면 고르던 목록이 그대로
 * 남아야 하므로 두 단계의 값을 함께 든다.
 */
@Immutable
data class TimelinePlaceSheetState(
    val timelineEventId: Long,
    val eventType: TimelineEventType,
    /** 시트 위 이벤트 맥락 줄. 예: `12:20 ~ 13:20 · 점심 식사`. */
    val eventLabel: String,
    val currentPlace: String,
    /**
     * 목록에 보일 장소. 서버 후보 순서 그대로이되, 지금 장소가 후보에 없으면(직접 입력했던 값) 맨 위에
     * 둔다 — 없으면 지금 무엇으로 돼 있는지 시트에서 보이지 않는다.
     */
    val options: List<String>,
    val selected: String,
    val step: Step = Step.LIST,
    val directInput: String = "",
) {
    enum class Step {
        LIST,
        DIRECT_INPUT,
    }

    /** 직접 입력으로 정할 장소. 공백뿐이거나 길이를 넘으면 null 이라 확인을 막는다. */
    val directPlace: String?
        get() =
            directInput
                .trim()
                .takeIf { it.isNotEmpty() && it.length <= UpdateTimelineEventPlaceUseCase.MAX_PLACE_LENGTH }

    /** 확인을 누르면 정해질 장소. */
    val chosenPlace: String?
        get() =
            when (step) {
                Step.LIST -> selected
                Step.DIRECT_INPUT -> directPlace
            }
}
