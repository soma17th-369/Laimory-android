package com.soma369.laimory.core.domain.model.analytics

/**
 * 타임라인에 들어온 자리.
 *
 * 타임라인 화면 인자로 함께 넘긴다 — 화면은 누가 자기를 열었는지 알 수 없어서, 여는 쪽이 적어 준다.
 * 적지 않고 연 경로는 [UNKNOWN] 이다.
 */
enum class AnalyticsEntryPoint {
    /** 지난 기록 목록. */
    PAST_RECORDS,

    /** 캘린더에서 날짜를 골랐다. */
    CALENDAR,

    /** 홈의 `타임라인 확인하기`. 생성 준비도 홈에서만 시작한다. */
    HOME,

    /** 생성이 끝나 자동으로 넘어갔거나, 완료 스낵바·완료 알림으로 열었다. */
    DRAFT_COMPLETE,

    UNKNOWN,
}
