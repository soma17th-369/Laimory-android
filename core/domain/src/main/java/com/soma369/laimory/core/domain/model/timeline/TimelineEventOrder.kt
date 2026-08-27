package com.soma369.laimory.core.domain.model.timeline

/**
 * 하루 타임라인이 이벤트를 보여 주는 순서 — 시작 시각 오름차순.
 *
 * **서버는 순서를 보장하지 않는다.** 조회 응답의 배열 순서를 그대로 쓰면 새로 만든 이벤트가
 * 하루 중간 시각인데도 목록 끝에 붙는 식으로 화면이 흔들린다. 그래서 조회와 세션 병합이 같은
 * 이 규칙으로 정렬한다.
 *
 * 시각이 같으면 `timelineEventId` 로 가른다. 이벤트 생성은 겹침 보정이 없어 같은 시각이 실제로
 * 생기고, 2차 키가 없으면 같은 데이터를 볼 때마다 순서가 바뀐다.
 */
val TIMELINE_EVENT_DISPLAY_ORDER: Comparator<TimelineEvent> =
    compareBy<TimelineEvent> { it.startAt }.thenBy { it.timelineEventId }

/** 표시 순서로 정렬한다. */
fun List<TimelineEvent>.sortedForDisplay(): List<TimelineEvent> = sortedWith(TIMELINE_EVENT_DISPLAY_ORDER)
