package com.soma369.laimory.feature.home.draft

import java.time.Instant
import java.time.LocalDate

/**
 * 서버가 작업 번호를 주기 전, 초안 생성 요청 한 번의 상태.
 *
 * 작업 번호를 받은 뒤의 추적은 `DraftTaskCoordinator` 가 맡는다. 그 전 구간은 거기에 자리가 없어 따로 둔다 —
 * `만들기` 를 누르면 곧바로 로딩 화면으로 넘어가므로, 요청을 보내는 동안과 실패를 홈과 로딩 화면이 함께 봐야 한다.
 */
sealed interface DraftSubmission {
    data object Idle : DraftSubmission

    /** 사진 업로드 · 초안 생성 요청을 보내는 중이다. [startedAt] 은 연출 경과의 기준이다. */
    data class Submitting(
        val recordDate: LocalDate,
        val startedAt: Instant,
    ) : DraftSubmission

    /**
     * 작업 번호를 받지 못하고 끝났다. 홈이 [error] 로 돌아간 뒤의 처리를 하고 비운다.
     *
     * @property shownOnLoading 로딩 화면이 이 실패를 안내했고 사용자가 확인해 돌아왔다. 홈은 같은 문구를 스낵바로
     *   되풀이하지 않는다.
     */
    data class Failed(
        val recordDate: LocalDate,
        val error: Throwable,
        val kind: DraftSubmitFailureKind,
        val shownOnLoading: Boolean = false,
    ) : DraftSubmission
}
