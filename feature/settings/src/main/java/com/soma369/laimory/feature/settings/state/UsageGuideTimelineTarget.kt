package com.soma369.laimory.feature.settings.state

import java.time.LocalDate

/**
 * `사용 방법 › 타임라인 편집 둘러보기` 가 열 기록. 시트를 열 때 기록 목록을 받아 정한다.
 *
 * 코치마크는 실제 화면을 짚으므로 이벤트가 있는 기록이 있어야 한다. 없으면 항목을 막고 이유를 적는다.
 */
sealed interface UsageGuideTimelineTarget {
    data object Loading : UsageGuideTimelineTarget

    /** 이벤트가 있는 가장 최근 기록. */
    data class Available(
        val recordDate: LocalDate,
    ) : UsageGuideTimelineTarget

    /** 이벤트가 있는 기록이 하나도 없다. */
    data object Unavailable : UsageGuideTimelineTarget

    /** 기록 목록을 받지 못했다. 누르면 다시 받는다. */
    data object Failed : UsageGuideTimelineTarget
}
