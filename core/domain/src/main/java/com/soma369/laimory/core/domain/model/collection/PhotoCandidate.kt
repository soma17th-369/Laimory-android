package com.soma369.laimory.core.domain.model.collection

import java.time.Instant

/**
 * 수집 대상 후보 사진 1건. 아직 저장(스테이징)되지 않은, MediaStore 에서 조회만 된 사진이다.
 *
 * 선택 수집 UI(썸네일 그리드)가 이 목록을 그리고, 사용자가 고른 [id] 들을 수집 요청으로 넘긴다.
 * 저장 단위인 [SourceItem] 과 달리 EXIF/위치 등 비싼 값은 담지 않는다 — 실제 수집 시점에 읽는다.
 *
 * - [id]: MediaStore `_ID`. 수집 요청과 `sourceKey` 파생의 기준.
 * - [contentUri]: 썸네일 로딩용 content URI 문자열(`ContentUris.withAppendedId(...)`).
 * - [takenAt]: `DATE_TAKEN` 기준 촬영 시각. 날짜 선택/정렬의 기준이다.
 */
data class PhotoCandidate(
    val id: Long,
    val contentUri: String,
    val takenAt: Instant,
)
