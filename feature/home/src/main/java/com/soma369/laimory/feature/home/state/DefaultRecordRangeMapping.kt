package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.domain.model.settings.DefaultRecordRange
import java.time.LocalTime

/** 저장 모델의 익일 여부를 홈이 쓰는 당일·익일로 옮긴다. */
internal fun DefaultRecordRange.endDay(): DraftEndDay = if (endsNextDay) DraftEndDay.NEXT_DAY else DraftEndDay.SAME_DAY

/** 홈이 들고 있는 세 값을 저장 모델로 묶는다. */
internal fun defaultRecordRangeOf(
    startTime: LocalTime,
    endDay: DraftEndDay,
    endTime: LocalTime,
): DefaultRecordRange = DefaultRecordRange(startTime = startTime, endsNextDay = endDay == DraftEndDay.NEXT_DAY, endTime = endTime)

/** 홈에 확정된 범위. */
internal val HomeUiState.recordRange: DefaultRecordRange
    get() = defaultRecordRangeOf(startTime, endDay, endTime)
