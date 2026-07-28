package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable
import com.soma369.laimory.feature.home.model.PastRecordUiModel

/** 홈 지난 기록 섹션 상태. 오늘의 초안 카드 상태와 독립적으로 동작한다. */
@Immutable
sealed interface HomePastRecordsUiState {
    data object Loading : HomePastRecordsUiState

    /** 서버 전체 조회 결과가 빈 목록인 경우. */
    data object Empty : HomePastRecordsUiState

    /** 네트워크 오류 등으로 목록 조회에 실패한 경우. 다시 시도할 수 있다. */
    data object LoadFailed : HomePastRecordsUiState

    /** 서버 정렬을 보존한 전체 기록 목록. */
    data class Content(
        val records: List<PastRecordUiModel>,
    ) : HomePastRecordsUiState
}
