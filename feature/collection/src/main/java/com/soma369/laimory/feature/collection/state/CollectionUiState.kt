package com.soma369.laimory.feature.collection.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.core.domain.model.collection.PhotoCandidate
import com.soma369.laimory.core.domain.model.collection.SourceItem
import com.soma369.laimory.core.ui.base.UiState
import java.time.LocalDate

@Immutable
data class CollectionUiState(
    /** 저장소 첫 emission 전까지 로딩으로 둔다. (빈 상태 문구 선노출 방지) */
    val isLoading: Boolean = true,
    /** 스테이징된 사진 아이템(저장소 관찰 결과에서 PHOTO 만 필터). */
    val stagedPhotos: List<SourceItem> = emptyList(),
    /** 마지막으로 고른 날짜. null 이면 아직 날짜 선택 전. */
    val selectedDate: LocalDate? = null,
    /** [selectedDate] 에 촬영된 후보 사진. "그날 전부 수집" 대상이자 선택 그리드의 표시 목록. */
    val candidates: List<PhotoCandidate> = emptyList(),
    /** 선택 수집 바텀시트 표시 여부. */
    val isSheetVisible: Boolean = false,
    /** 바텀시트 그리드에서 사용자가 고른 사진 id. */
    val selectedIds: Set<Long> = emptySet(),
    /** 후보 조회/수집이 진행 중인지. 버튼 비활성/진행표시에 사용. */
    val isBusy: Boolean = false,
) : UiState
