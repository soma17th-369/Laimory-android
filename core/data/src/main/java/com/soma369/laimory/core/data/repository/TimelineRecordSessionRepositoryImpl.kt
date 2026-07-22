package com.soma369.laimory.core.data.repository

import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.domain.model.timeline.TimelineEvent
import com.soma369.laimory.core.domain.repository.TimelineRecordSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class TimelineRecordSessionRepositoryImpl
    @Inject
    constructor() : TimelineRecordSessionRepository {
        private val mutableTimeline = MutableStateFlow<DailyTimeline?>(null)

        override val timeline: StateFlow<DailyTimeline?> = mutableTimeline.asStateFlow()

        override fun save(timeline: DailyTimeline) {
            mutableTimeline.value = timeline
        }

        override fun replaceEvent(event: TimelineEvent) {
            mutableTimeline.update { current ->
                val eventIndex = current?.events?.indexOfFirst { it.timelineEventId == event.timelineEventId } ?: -1
                if (current == null || eventIndex < 0) {
                    current
                } else {
                    current.copy(
                        events = current.events.toMutableList().apply { set(eventIndex, event) },
                    )
                }
            }
        }

        override fun removeEvent(timelineEventId: Long) {
            mutableTimeline.update { current ->
                current?.copy(events = current.events.filterNot { it.timelineEventId == timelineEventId })
            }
        }

        override fun clear() {
            mutableTimeline.value = null
        }
    }
