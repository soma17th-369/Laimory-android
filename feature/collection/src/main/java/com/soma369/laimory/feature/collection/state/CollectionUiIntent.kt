package com.soma369.laimory.feature.collection.state

import com.soma369.laimory.core.ui.base.UiIntent
import java.time.LocalDate

sealed interface CollectionUiIntent : UiIntent {
    /** 날짜를 골라 그날 촬영된 사진 후보를 불러온다. */
    data class SelectDate(val date: LocalDate) : CollectionUiIntent

    /** 선택된 날짜의 후보 전부를 수집한다. */
    data object CollectAllOnDate : CollectionUiIntent

    /** 선택 수집 바텀시트를 연다. */
    data object OpenSelectSheet : CollectionUiIntent

    /** 선택 수집 바텀시트를 닫는다. */
    data object DismissSelectSheet : CollectionUiIntent

    /** 바텀시트 그리드에서 사진 하나의 선택을 토글한다. */
    data class ToggleCandidate(val id: Long) : CollectionUiIntent

    /** 바텀시트에서 고른 사진들을 수집한다. */
    data object CollectSelected : CollectionUiIntent

    /** 스테이징된 사진을 모두 비운다(일괄 삭제). */
    data object ClearStaged : CollectionUiIntent
}
