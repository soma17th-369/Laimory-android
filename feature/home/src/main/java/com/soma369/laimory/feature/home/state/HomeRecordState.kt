package com.soma369.laimory.feature.home.state

/**
 * 고른 날짜에 서버가 들고 있는 하루 기록. 홈 CTA 가 `타임라인 확인하기` 를 띄울지 정한다.
 *
 * 월별 조회의 `status` 만으로는 부족하다 — 서버는 초안 생성 요청을 받으면 AI 로 보내기 전에
 * DailyRecord 를 먼저 만들고, 실패한 작업이 남긴 빈 DRAFT 도 지우지 않는다. 그래서 초안은 이벤트가
 * 있는지까지 본다.
 */
enum class HomeRecordState(
    /** 열어 볼 기록이 있다. CTA 를 누르면 그 날짜의 타임라인으로 간다. */
    val isViewable: Boolean,
) {
    /** 기록이 없거나 아직 모른다. */
    NONE(isViewable = false),

    /** DRAFT 는 있지만 이벤트가 없다 — 생성 중이거나 실패해 남은 껍데기다. */
    EMPTY_DRAFT(isViewable = false),

    /** 아직 저장하기를 누르지 않은 초안. */
    DRAFT(isViewable = true),

    /** 저장이 끝난 기록. */
    SAVED(isViewable = true),
}
