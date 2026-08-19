package com.soma369.laimory.feature.home.draft

import com.soma369.laimory.core.domain.model.collection.ItemType
import com.soma369.laimory.core.domain.model.collection.PhotoPayload
import com.soma369.laimory.core.domain.model.timeline.DraftSourceItemSelection
import java.time.LocalDate

/**
 * 실제로 전송되는 선택 결과에서 로딩 화면 스냅샷을 만든다.
 *
 * 건수는 사용자 제외를 반영한 `selectedCounts`를 쓴다 — 화면이 "제출된" 수를 말하기 때문이다.
 * 사진 URI는 전송 순서를 유지해, 콜라주가 앞에서부터 채워도 타임라인 순서와 어긋나지 않는다.
 */
internal fun DraftSourceItemSelection.toLoadingSession(
    taskId: String,
    recordDate: LocalDate,
): DraftLoadingSession =
    DraftLoadingSession(
        taskId = taskId,
        recordDate = recordDate,
        photoUris = items.mapNotNull { (it.payload as? PhotoPayload)?.clientPhotoUri },
        photoCount = report.selectedCounts.getOrDefault(ItemType.PHOTO, 0),
        calendarCount = report.selectedCounts.getOrDefault(ItemType.CALENDAR, 0),
        stayCount = report.selectedCounts.getOrDefault(ItemType.STAY, 0),
    )
