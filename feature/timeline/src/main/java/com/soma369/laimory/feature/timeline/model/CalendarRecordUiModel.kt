package com.soma369.laimory.feature.timeline.model

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.timeline.MonthlyDailyRecord
import com.soma369.laimory.core.ui.model.toUiEmotionOrNull
import com.soma369.laimory.core.ui.theme.Emotion
import java.time.LocalDate

/**
 * 캘린더 날짜 셀 한 칸이 쓰는 최소 표시 모델.
 *
 * 셀은 기록 유무와 감정만 보여주므로 월별 응답 원본을 화면 상태에 그대로 들이지 않는다.
 * 실제 기록은 날짜를 선택할 때 단건 조회로 다시 받는다 — 이 모델은 정본이 아니다.
 */
@Immutable
data class CalendarRecordUiModel(
    val recordDate: LocalDate,
    /** 서버 감정을 표시 팔레트로 옮긴 값. 감정이 없거나 미지 literal 이면 null(중립 표시). */
    val emotion: Emotion?,
)

/**
 * 월별 조회 결과를 날짜별 기록으로 옮긴다.
 *
 * 서버가 `(subject_id, record_date)` 를 유일하게 두므로 같은 날짜가 두 번 오지 않는다 — 대표를 고르는
 * 정책이 필요 없다.
 */
internal fun List<MonthlyDailyRecord>.toCalendarRecordsByDate(): Map<LocalDate, CalendarRecordUiModel> =
    associate { record ->
        record.recordDate to
            CalendarRecordUiModel(
                recordDate = record.recordDate,
                emotion = record.emotion?.toUiEmotionOrNull(),
            )
    }
