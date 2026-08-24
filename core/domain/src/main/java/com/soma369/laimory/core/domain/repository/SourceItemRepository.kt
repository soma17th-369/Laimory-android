package com.soma369.laimory.core.domain.repository

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.SourceItem
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * 수집 아이템 로컬 저장 계약.
 *
 * 구현은 `:core:collection` 이 소유한다 — 수집 영역의 저장(Room)은
 * `:core:data` 의 remote 책임과 분리해 수집 모듈이 자체 소유한다.
 */
interface SourceItemRepository {
    /**
     * 아이템을 저장한다. `sourceKey` 가 이미 존재하는 아이템은 무시한다(멱등).
     * 사진·일정처럼 값이 변하지 않는 불변 이벤트에 쓴다.
     *
     * @return 실제로 새로 저장된 아이템 수
     */
    suspend fun addAll(items: List<SourceItem>): Int

    /**
     * 아이템을 upsert 한다. 이미 있는 `sourceKey` 는 최초 `rawId` 를 유지한 채 값을 갱신한다.
     * 걸음수 일별 집계처럼 재수집 때마다 값이 변하는 aggregate 에 쓴다.
     *
     * @return 새로 삽입된(갱신 제외) 아이템 수
     */
    suspend fun upsertAll(items: List<SourceItem>): Int

    /** 저장된 전체 아이템을 이벤트 시각 내림차순으로 관찰한다. */
    fun observeAll(): Flow<List<SourceItem>>

    /**
     * 기록 창 `[start, end)` 와 겹치는 아이템을 **한 번** 읽는다.
     *
     * 자동 수집 직후 전송 스냅샷을 확정할 때 쓴다. [observeAll] 의 첫 emission 을 기다리는 방식은
     * 관찰과 조회를 섞어 "수집이 반영된 값인지" 를 호출부가 보장할 수 없다.
     *
     * 포함 규칙은
     * [com.soma369.laimory.core.domain.model.timeline.RecordDateWindow.contains] 와 같다 —
     * 단일 시점은 `start <= startAt < end`, 구간은 `startAt < end && endAt > start`.
     * 자정을 걸친 수면·일정은 겹치므로 딸려오고, 경계에 맞닿기만 한 구간은 빠진다.
     */
    suspend fun getInWindow(
        start: Instant,
        end: Instant,
    ): List<SourceItem>

    /** 해당 카테고리에서 마지막으로 수집된 시각. 증분 수집 커서로 사용한다. 없으면 null. */
    suspend fun getLatestCollectedAt(itemType: ItemType): Instant?

    /**
     * 이벤트 시각이 [cutoff] 이전 보존 범위에 속하는 아이템을 삭제한다.
     *
     * 단일 시점 이벤트는 `startAt < cutoff`, 구간 이벤트는 반열린 구간 `[startAt, endAt)`을 기준으로
     * `endAt <= cutoff`일 때 삭제한다. 따라서 cutoff를 걸치는 구간 이벤트는 유지한다.
     *
     * @return 삭제된 아이템 수
     */
    suspend fun deleteExpired(cutoff: Instant): Int

    /** 해당 카테고리에 저장된 아이템을 모두 삭제한다(스테이징 일괄 비우기). */
    suspend fun clear(itemType: ItemType)
}
