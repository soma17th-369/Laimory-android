package com.soma369.laimory.feature.home.loading

import com.soma369.laimory.core.ui.base.UiIntent

sealed interface DraftLoadingUiIntent : UiIntent {
    /** 뒤로가기. 작업은 취소하지 않고 홈으로만 돌아간다. */
    data object NavigateBack : DraftLoadingUiIntent

    /** 재시도 가능한 오류에서 다시 상태를 조회한다. */
    data object Retry : DraftLoadingUiIntent

    /** 장기 실행 안내에서 계속 기다린다. */
    data object ContinueWaiting : DraftLoadingUiIntent

    /** 추적을 접고 홈으로 돌아간다. */
    data object Discard : DraftLoadingUiIntent
}
