package com.soma369.laimory.core.domain.coordinator

import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/** 화면과 독립적으로 현재 초안 작업의 polling·복구·결과 저장을 조정한다. */
interface DraftTaskCoordinator {
    /** 현재 추적 중인 초안 작업의 상태다. */
    val state: StateFlow<DraftTaskTrackingState>

    /**
     * 새 초안 생성 작업을 추적하고 활성 작업으로 영속화한다.
     *
     * @param taskId 서버가 발급한 비동기 초안 작업 식별자
     * @param recordDate 초안을 생성할 하루 기록의 날짜
     */
    suspend fun start(
        taskId: String,
        recordDate: LocalDate,
    )

    /** 앱이 전경으로 진입했을 때 저장된 활성 작업을 복구하고 polling을 재개한다. */
    suspend fun onForeground()

    /** 앱이 배경으로 전환됐을 때 활성 작업은 유지하면서 전경 polling을 중단한다. */
    suspend fun onBackground()

    /**
     * FCM 완료 신호가 현재 활성 작업을 가리키면 서버 상태를 즉시 한 번 재조회한다.
     *
     * 이 호출은 화면을 직접 이동시키지 않는다. 앱이 배경 상태라면 신호를 보류하고,
     * 다음 전경 진입 시 상태를 확인한다. 현재 활성 작업과 다른 [taskId]는 무시한다.
     *
     * @param taskId FCM data payload에 포함된 비동기 초안 작업 식별자
     */
    fun refreshFromCompletionSignal(taskId: String)

    /** 재시도 가능한 오류 상태의 활성 작업을 다시 polling한다. */
    fun retry()

    /** 장기 실행 안내 이후 사용자가 대기를 선택하면 제한 없이 polling을 재개한다. */
    fun continueWaiting()

    /** 활성 작업과 저장된 추적 정보를 제거하고 초기 상태로 되돌린다. */
    suspend fun discard()
}
