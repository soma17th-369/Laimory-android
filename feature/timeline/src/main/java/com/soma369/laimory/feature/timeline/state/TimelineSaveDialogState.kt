package com.soma369.laimory.feature.timeline.state

import androidx.compose.runtime.Immutable

/** 작성 완료 확인 다이얼로그 상태. 성공은 별도 상태 없이 홈 복귀와 스낵바로 안내한다. */
@Immutable
sealed interface TimelineSaveDialogState {
    data object Hidden : TimelineSaveDialogState

    data object Confirmation : TimelineSaveDialogState

    data object Saving : TimelineSaveDialogState

    data class RetryableError(
        val message: String,
    ) : TimelineSaveDialogState
}
