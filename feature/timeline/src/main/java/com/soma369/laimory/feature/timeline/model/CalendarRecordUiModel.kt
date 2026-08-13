package com.soma369.laimory.feature.timeline.model

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.timeline.DailyTimeline
import com.soma369.laimory.core.ui.model.toUiEmotionOrNull
import com.soma369.laimory.core.ui.theme.Emotion
import java.time.LocalDate

/**
 * 캘린더 날짜 셀 한 칸이 쓰는 최소 표시 모델.
 *
 * 셀은 기록 유무와 감정만 보여주므로 `DailyTimeline.events` graph 와 응답 원본은 화면 상태에 들이지 않는다.
 * 실제 기록은 날짜를 선택할 때 단건 조회로 다시 받는다 — 이 모델은 정본이 아니다.
 */
@Immutable
data class CalendarRecordUiModel(
    val recordDate: LocalDate,
    /** 서버 감정을 표시 팔레트로 옮긴 값. 감정이 없거나 미지 literal 이면 null(중립 표시). */
    val emotion: Emotion?,
)

/**
 * 서버 전체 조회 결과를 날짜별 대표 기록으로 접는다.
 *
 * 같은 `recordDate` 가 여러 건이면 서버 정렬상 **첫 번째** 기록을 대표로 쓴다.
 * (`associateBy` 는 마지막 값으로 조용히 덮어써 이 정책을 뒤집으므로 쓰지 않는다.)
 */
internal fun List<DailyTimeline>.toCalendarRecordsByDate(): Map<LocalDate, CalendarRecordUiModel> =
    buildMap {
        this@toCalendarRecordsByDate.forEach { timeline ->
            if (containsKey(timeline.recordDate)) return@forEach
            put(
                timeline.recordDate,
                CalendarRecordUiModel(
                    recordDate = timeline.recordDate,
                    emotion = timeline.emotion?.toUiEmotionOrNull(),
                ),
            )
        }
    }
