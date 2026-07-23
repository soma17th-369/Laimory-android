package com.soma369.laimory.feature.timeline.component

import androidx.annotation.DrawableRes
import com.soma369.laimory.core.domain.model.timeline.TimelineEventType
import com.soma369.laimory.core.ui.R

internal val TimelineEventTypeDisplayOrder =
    listOf(
        TimelineEventType.WAKE_UP,
        TimelineEventType.MOVEMENT,
        TimelineEventType.MEAL,
        TimelineEventType.MEETING,
        TimelineEventType.WORK,
        TimelineEventType.EXERCISE,
        TimelineEventType.SOCIAL,
        TimelineEventType.REST,
        TimelineEventType.SLEEP,
        TimelineEventType.CALENDAR_EVENT,
        TimelineEventType.PHOTO_MOMENT,
        TimelineEventType.CLASS,
        TimelineEventType.UNKNOWN,
    )

internal fun TimelineEventType.displayLabel(): String =
    when (this) {
        TimelineEventType.WAKE_UP -> "기상"
        TimelineEventType.SLEEP -> "수면"
        TimelineEventType.MOVEMENT -> "이동"
        TimelineEventType.CALENDAR_EVENT -> "일정"
        TimelineEventType.MEAL -> "식사"
        TimelineEventType.PHOTO_MOMENT -> "사진"
        TimelineEventType.MEETING -> "회의"
        TimelineEventType.CLASS -> "수업"
        TimelineEventType.WORK -> "업무"
        TimelineEventType.EXERCISE -> "운동"
        TimelineEventType.SOCIAL -> "만남"
        TimelineEventType.REST -> "휴식"
        TimelineEventType.UNKNOWN -> "기타"
    }

@DrawableRes
internal fun TimelineEventType.iconResource(): Int =
    when (this) {
        TimelineEventType.WAKE_UP -> R.drawable.ico_timeline_event_wake_up
        TimelineEventType.SLEEP -> R.drawable.ico_timeline_event_sleep
        TimelineEventType.MOVEMENT -> R.drawable.ico_timeline_event_movement
        TimelineEventType.CALENDAR_EVENT -> R.drawable.ico_timeline_event_calendar_event
        TimelineEventType.MEAL -> R.drawable.ico_timeline_event_meal
        TimelineEventType.PHOTO_MOMENT -> R.drawable.ico_timeline_event_photo_moment
        TimelineEventType.MEETING -> R.drawable.ico_timeline_event_meeting
        TimelineEventType.CLASS -> R.drawable.ico_timeline_event_class
        TimelineEventType.WORK -> R.drawable.ico_timeline_event_work
        TimelineEventType.EXERCISE -> R.drawable.ico_timeline_event_exercise
        TimelineEventType.SOCIAL -> R.drawable.ico_timeline_event_social
        TimelineEventType.REST -> R.drawable.ico_timeline_event_rest
        TimelineEventType.UNKNOWN -> R.drawable.ico_timeline_event_unknown
    }
