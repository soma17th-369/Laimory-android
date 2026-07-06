package com.soma369.laimory.core.domain.collector

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.SourceItem

/**
 * 카테고리별 수집 실행 계약.
 *
 * 각 카테고리 수집기(PhotoCollector, CalendarCollector 등)는 이 인터페이스를 구현하고,
 * 수집 결과를 [SourceItem] 목록으로 반환한다. 저장은 수집기의 책임이 아니다 —
 * 호출자가 SourceItemRepository 를 통해 저장한다.
 *
 * 권한 확인/요청은 수집기 밖(UI 계층)의 책임이며, 권한이 없는 상태의 [collect] 는
 * 예외 대신 빈 목록을 반환한다.
 */
interface Collector {
    /** 이 수집기가 담당하는 카테고리. 증분 수집 커서 조회의 기준이 된다. */
    val itemType: ItemType

    /** 현재 수집 가능한 아이템을 모아 반환한다. 중복 제거는 저장 계층의 sourceKey 가 보장한다. */
    suspend fun collect(): List<SourceItem>
}
