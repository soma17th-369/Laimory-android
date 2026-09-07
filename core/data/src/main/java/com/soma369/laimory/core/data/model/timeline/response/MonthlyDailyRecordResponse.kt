package com.soma369.laimory.core.data.model.timeline.response

import com.soma369.laimory.core.domain.model.timeline.DailyRecordStatus
import com.soma369.laimory.core.domain.model.timeline.MonthlyDailyRecord
import kotlinx.serialization.Serializable

/**
 * 월별 조회 항목. 서버는 `recordDate` 와 non-null `status`, nullable `emotionType` 을 내려준다.
 *
 * `dailyRecordId`·`events` 는 응답에 없다 — 캘린더 셀은 기록 유무와 감정, 초안/저장 구분만 그린다.
 *
 * [status] 를 nullable 로 받는 이유는 값이 아니라 **배포 순서** 때문이다. 서버가 필드를 빼거나
 * 모르는 literal 을 보내면 여기서 `null` 이 되고, 화면은 그 날짜를 초안이 아닌 것으로 다룬다 —
 * 모르는 값을 초안 쪽으로 흘리면 이미 있는 기록에 새 초안을 만들려 든다.
 */
@Serializable
data class MonthlyDailyRecordResponse(
    val recordDate: String,
    val status: String? = null,
    val emotionType: String? = null,
)

internal fun MonthlyDailyRecordResponse.toDomain(): MonthlyDailyRecord =
    MonthlyDailyRecord(
        recordDate = recordDate.parseLocalDate("recordDate"),
        status = status?.let { raw -> DailyRecordStatus.entries.firstOrNull { it.name == raw } },
        emotion = emotionType.toTimelineEmotionOrNull(),
    )
