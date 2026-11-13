package com.soma369.laimory.feature.home.state

import androidx.compose.runtime.Immutable

/**
 * 실행 취소할 수 있는 삭제 묶음.
 *
 * 연달아 지우면 스낵바를 새로 띄우지 않고 이 묶음에 건수를 더한다. 실행 취소는 묶음 전체를 되살린다.
 * [token] 은 지울 때마다 바뀐다 — 화면이 스낵바를 다시 시작하고, 이미 지나간 묶음의 실행 취소·만료
 * 신호를 가려내는 데 쓴다.
 */
@Immutable
data class DraftConsentDeletionUndo(
    val token: Long,
    val count: Int,
)
