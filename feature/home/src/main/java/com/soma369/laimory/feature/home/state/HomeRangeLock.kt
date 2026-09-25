package com.soma369.laimory.feature.home.state

/**
 * 날짜 피커에서 임시로 고른 날의 기록 범위를 바꿀 수 있는지.
 *
 * 월별 도트(`savedRecordDates`·`draftRecordDates`)로 정하지 않는다 — 월별 응답에는 이벤트가 없어
 * 생성에 실패해 남은 빈 초안도 초안으로 들어오고, 새 달을 연 직후나 조회가 실패하면 비어 있다.
 * 확정 후 CTA 와 같은 단건 판정([HomeRecordState.isViewable])을 쓴다.
 */
enum class HomeRangeLock {
    /** 단건 판정을 기다리는 중. 칩은 열어 두되 바뀐 범위는 판정이 끝나야 확정한다. */
    CHECKING,

    /** 기록이 없거나 빈 초안이다. 범위를 바꿀 수 있다. */
    EDITABLE,

    /** 저장된 기록이나 내용 있는 초안이 있다. 만들 것이 없어 범위를 바꾸지 않는다. */
    LOCKED,

    /** 판정 조회가 실패했다. 칩은 열어 두지만 바뀐 범위는 확정하지 않는다. */
    FAILED,
}
