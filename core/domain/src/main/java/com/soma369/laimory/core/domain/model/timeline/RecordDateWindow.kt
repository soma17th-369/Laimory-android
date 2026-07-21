package com.soma369.laimory.core.domain.model.timeline

import com.soma369.laimory.core.domain.model.collection.SourceItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 선택한 날짜를 기준으로 사용자가 고른 "기록 창" — [start] 이상 [end] 미만의 반열린 구간 [start, end).
 *
 * 기본 창은 선택 날짜의 자정부터 다음 날 자정까지(달력 하루)지만, 사용자가 시작·종료 시각을
 * 바꾸면 종료가 익일까지 확장될 수 있다. 타임라인 도메인 개념이며
 * 수집(collection)이 아니라 타임라인이 소유한다 — 수집 아이템([SourceItem])을 소비할 뿐이다.
 * 홈/타임라인 요약과 초안 생성 입력(#120)이 같은 창을 공유하므로 창 구성은 [ofDate] 한 곳에서 만든다.
 *
 * 포함 판정([contains])은 구간이 겹치기만 하면 참이라, 자정을 걸친 수면·일정처럼
 * 창 경계를 넘나드는 아이템도 그날 데이터로 딸려 들어온다.
 */
data class RecordDateWindow(
    val start: Instant,
    val end: Instant,
) {
    init {
        require(start < end) { "기록 창의 종료 시각은 시작 시각보다 뒤여야 합니다." }
    }

    /**
     * [item] 의 시간이 이 창 [start, end) 와 겹치면 true.
     *
     * - 단일 시점 이벤트(사진·알림 등 `endAt == null`): 시점이 창 안(`start <= t < end`)이면 포함.
     * - 구간 이벤트(수면·일정·걸음수 day-bucket 등): 두 반열린 구간이 실제로 겹치면
     *   (`startAt < end && endAt > start`) 포함한다. 자정을 걸친 수면·일정은 겹치므로 그날로 딸려오고,
     *   창 경계에 맞닿기만 한 구간(예: 어제 걸음수 bucket 이 오늘 00:00 에 끝남)은 제외돼
     *   인접한 두 날에 이중 계수되지 않는다.
     */
    fun contains(item: SourceItem): Boolean {
        val itemEnd = item.endAt
        return if (itemEnd == null) {
            item.startAt >= start && item.startAt < end
        } else {
            item.startAt < end && itemEnd > start
        }
    }

    companion object {
        /** [date] 하루의 창 [자정, 다음 날 자정). 자정 경계는 [zone] 기준으로 잡는다. */
        fun ofDate(
            date: LocalDate,
            zone: ZoneId,
        ): RecordDateWindow =
            RecordDateWindow(
                start = date.atStartOfDay(zone).toInstant(),
                end = date.plusDays(1).atStartOfDay(zone).toInstant(),
            )
    }
}
