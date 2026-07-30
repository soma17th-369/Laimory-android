package com.soma369.laimory.core.domain.source

import com.soma369.laimory.core.domain.model.collection.PhotoCandidate
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.domain.model.timeline.RecordDateWindow
import java.time.LocalDate
import java.time.ZoneId

/**
 * 날짜 기반 사진 조회·선택 수집 포트.
 *
 * 전량 배치 수집(`Collector`)과 달리, 사용자가 고른 날짜/사진만 선택적으로 수집하기 위한 계약이다.
 * 권한 확인/요청은 포트 밖(UI 계층)의 책임이며, 권한이 없거나 조회 실패 시 예외 대신 빈 목록을 반환한다.
 * 저장은 포트의 책임이 아니다 — 호출자가 SourceItemRepository 로 저장한다.
 */
interface PhotoSource {
    /** [window]에 촬영된 사진 후보를 최신순으로 반환한다. */
    suspend fun photosIn(window: RecordDateWindow): List<PhotoCandidate>

    /** [date](기기 시간대, `DATE_TAKEN` 기준)에 촬영된 사진 후보를 최신순으로 반환한다. */
    suspend fun photosOn(date: LocalDate): List<PhotoCandidate> {
        return photosIn(RecordDateWindow.ofDate(date, ZoneId.systemDefault()))
    }

    /** [ids](MediaStore `_ID`)에 해당하는 사진을 EXIF 위치까지 읽어 [SourceItem] 으로 변환한다. */
    suspend fun collect(ids: List<Long>): List<SourceItem>
}
