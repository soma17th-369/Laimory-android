package com.soma369.laimory.core.domain.source

import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import kotlinx.coroutines.flow.Flow

/**
 * 시스템 캘린더 일정 조회 포트.
 *
 * 일정은 로컬 DB 에 복사해 두지 않고 볼 때마다 원본에서 읽는다. 원본이 언제든 다시 읽을 수 있게 남아 있어,
 * 캘린더에서 지난 일정을 고치거나 지우면 다음 조회에 그대로 반영된다 — 사진([PhotoSource])과 같은 방식이다.
 *
 * 권한 확인/요청은 포트 밖(UI 계층)의 책임이다.
 */
interface CalendarSource {
    /**
     * [window] 와 겹치는 개인 캘린더 일정 인스턴스를 시작 시각 순으로 읽는다. 포함 규칙은 [RecordDateWindow.contains] 다.
     * 종일 일정을 기록 날짜로 거르는 것은 호출자 몫이다([com.soma369.laimory.core.domain.model.timeline.CalendarAllDayRecordPolicy]).
     *
     * - 권한이 없으면 빈 목록이다. 카드의 허용 안내는 화면이 판정한 권한 상태로 가른다.
     * - 그 밖의 조회 실패는 예외로 올린다. 빈 목록으로 바꾸면 "일정이 없는 날" 과 구별되지 않는다.
     * - 같은 인스턴스는 읽을 때마다 같은 `rawId` 다. 상세에서 뺀 항목과 서버의 이어 붙이기 중복 판정이 이 값에 기댄다.
     */
    suspend fun eventsIn(window: RecordDateWindow): List<SourceItem>

    /**
     * 캘린더 내용이 바뀔 때마다 신호를 낸다. 구독 직후에는 내지 않는다.
     *
     * 권한을 켜는 것은 캘린더 내용의 변화가 아니라 신호가 오지 않는다 — 호출자가 권한 변화로 따로 다시 읽는다.
     */
    fun changes(): Flow<Unit>
}
