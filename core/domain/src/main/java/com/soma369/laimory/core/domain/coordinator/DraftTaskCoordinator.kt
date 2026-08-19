package com.soma369.laimory.core.domain.coordinator

import com.soma369.laimory.core.domain.model.timeline.DraftTaskCompletion
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/** 화면과 독립적으로 현재 초안 작업의 polling·복구·결과 저장을 조정한다. */
interface DraftTaskCoordinator {
    /** 현재 추적 중인 초안 작업의 상태다. */
    val state: StateFlow<DraftTaskTrackingState>

    /**
     * 아직 화면이 처리하지 않은 완료. 처리한 쪽이 [consumeCompletion]으로 비운다.
     *
     * 일회성 이벤트가 아니라 지속 상태다. 이벤트로 두면 발행 시점에 구독 중이던 쪽만 받으므로,
     * 콜드 스타트처럼 구독이 늦으면 완료가 사라지고 어느 화면이 받을지가 경합으로 정해진다.
     * 상태로 두면 늦게 붙은 구독자도 보고, 목적지 판단을 백스택이 정해진 뒤로 미룰 수 있다.
     *
     * [state]와 역할이 다르다. [state]의 `Success`는 계속 남아 홈의 `초안 보기` 같은 상시 UI가
     * 읽고, 이 값은 자동 이동·스낵바처럼 한 번만 해야 하는 처리를 위해 소비되면 사라진다.
     */
    val pendingCompletion: StateFlow<DraftTaskCompletion?>

    /**
     * [taskId]의 완료를 처리했다고 표시하고, 처리 권한을 얻었는지 돌려준다.
     *
     * 로딩 화면과 내비게이션 호스트가 같은 완료를 동시에 집을 수 있지만 실제로 화면을 옮기는 쪽은
     * 하나여야 한다. 원자적으로 비우고 비운 호출에만 `true`를 준다. 이미 소비됐거나 다른 작업의
     * 완료면 `false`다.
     */
    suspend fun consumeCompletion(taskId: String): Boolean

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
