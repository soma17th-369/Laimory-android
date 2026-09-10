package com.soma369.laimory.feature.home.state

import com.soma369.laimory.core.ui.base.UiIntent
import java.time.LocalDate

sealed interface PastRecordsUiIntent : UiIntent {
    /**
     * 진입·복귀(ON_RESUME) 시 서버와 재동기화한다.
     *
     * 기록을 열어 보고 돌아오거나 홈에서 초안을 만든 뒤 다시 들어오면 목록이 달라져 있다.
     * 이미 목록을 보여 주는 중이면 유지한 채 갱신한다(깜빡임 방지).
     */
    data object Sync : PastRecordsUiIntent

    /** 그 날짜의 타임라인 화면으로 간다. */
    data class SelectRecord(
        val recordDate: LocalDate,
    ) : PastRecordsUiIntent

    data object NavigateBack : PastRecordsUiIntent
}
