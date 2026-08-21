package com.soma369.laimory.feature.timeline.state

/**
 * 타임라인 기록 화면의 표시·조작 모드.
 *
 * 서버가 관리하는 하루 기록 상태(`DailyRecordStatus`)와 분리된 화면 상태다. 모드 전환은 서버 요청이나
 * `DRAFT ↔ SAVED` 전이를 일으키지 않으며, SAVED 기록도 편집 모드에 들어갈 수 있다.
 *
 * 최초 진입 모드만 기록 상태를 따른다 — DRAFT 는 [EDIT], SAVED 는 [READ] 다.
 */
enum class TimelineRecordMode {
    READ,
    EDIT,
    ;

    val isEditing: Boolean get() = this == EDIT
}
