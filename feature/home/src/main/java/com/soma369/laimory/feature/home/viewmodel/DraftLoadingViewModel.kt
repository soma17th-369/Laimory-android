package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.core.ui.base.UiSideEffect
import com.soma369.laimory.feature.home.draft.DraftLoadingSessionStore
import com.soma369.laimory.feature.home.loading.DraftLoadingAction
import com.soma369.laimory.feature.home.loading.DraftLoadingNotice
import com.soma369.laimory.feature.home.loading.DraftLoadingStage
import com.soma369.laimory.feature.home.loading.DraftLoadingStageMath
import com.soma369.laimory.feature.home.loading.DraftLoadingStageState
import com.soma369.laimory.feature.home.loading.DraftLoadingUiIntent
import com.soma369.laimory.feature.home.loading.DraftLoadingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toKotlinDuration

/**
 * 생성 로딩 화면.
 *
 * 작업 자체는 [DraftTaskCoordinator]가 화면과 무관하게 추적하므로, 이 ViewModel은 표시만 맡는다.
 * 완료 시 이동은 여기서 하지 않는다 — 로딩 화면을 보고 있지 않을 때도 완료가 오기 때문에 분기는
 * 한 곳(내비게이션 호스트)에서 한다.
 */
@HiltViewModel
class DraftLoadingViewModel
    @Inject
    constructor(
        private val coordinator: DraftTaskCoordinator,
        private val loadingSessionStore: DraftLoadingSessionStore,
        private val navigationHelper: NavigationHelper,
        private val clock: Clock,
    ) : BaseMviViewModel<DraftLoadingUiState, DraftLoadingUiIntent, UiSideEffect>(DraftLoadingUiState()) {
        init {
            observeTask()
            tickStages()
        }

        override suspend fun handleIntent(intent: DraftLoadingUiIntent) {
            when (intent) {
                DraftLoadingUiIntent.NavigateBack -> navigationHelper.navigateToBack()
                DraftLoadingUiIntent.Retry -> coordinator.retry()
                DraftLoadingUiIntent.ContinueWaiting -> coordinator.continueWaiting()
                DraftLoadingUiIntent.Discard -> {
                    coordinator.discard()
                    navigationHelper.navigateToBack()
                }
            }
        }

        private fun observeTask() {
            safeLaunch {
                combine(coordinator.state, loadingSessionStore.session) { tracking, session ->
                    tracking to session
                }.collect { (tracking, session) ->
                    val task = (tracking as? DraftTaskTrackingState.WithTask)?.task
                    val matched = task?.taskId?.let { loadingSessionStore.sessionFor(it) } ?: session
                    val isCompleted = tracking is DraftTaskTrackingState.Success
                    updateState {
                        copy(
                            recordDate = task?.recordDate ?: matched?.recordDate,
                            photoUris = matched?.photoUris.orEmpty(),
                            photoCount = matched?.photoCount ?: 0,
                            calendarCount = matched?.calendarCount ?: 0,
                            stayCount = matched?.stayCount ?: 0,
                            // 완료는 다음 연출 틱을 기다리지 않고 바로 보여준다. 화면이 넘어가기 전에
                            // 마지막 줄이 완료로 바뀌는 것을 알아볼 수 있어야 한다.
                            stageStates = if (isCompleted) ALL_STAGES_DONE else stageStates,
                            notice = tracking.toNotice(),
                        )
                    }
                    if (tracking is DraftTaskTrackingState.Success || tracking is DraftTaskTrackingState.Failed) {
                        task?.let { loadingSessionStore.clear(it.taskId) }
                    }
                }
            }
        }

        /**
         * 경과 시간에 따라 앞 세 줄을 차례로 완료로 바꾼다.
         *
         * 복원 시에도 화면 표시를 위해 서버를 다시 부르지 않는다 — 작업의 `requestedAt`으로 경과를
         * 계산하므로 재진입해도 연출이 처음부터 다시 시작하지 않는다.
         */
        private fun tickStages() {
            safeLaunch {
                while (true) {
                    val tracking = coordinator.state.value
                    val task = (tracking as? DraftTaskTrackingState.WithTask)?.task
                    val elapsed =
                        task
                            ?.let { Duration.between(it.requestedAt, clock.instant()) }
                            ?.toKotlinDuration()
                            ?.coerceAtLeast(kotlin.time.Duration.ZERO)
                            ?: kotlin.time.Duration.ZERO
                    val isCompleted = tracking is DraftTaskTrackingState.Success
                    updateState {
                        copy(
                            stageStates =
                                DraftLoadingStage.entries.associateWith { stage ->
                                    DraftLoadingStageMath.stateOf(stage, elapsed, isCompleted)
                                },
                        )
                    }
                    delay(STAGE_TICK)
                }
            }
        }

        private fun DraftTaskTrackingState.toNotice(): DraftLoadingNotice? =
            when (this) {
                DraftTaskTrackingState.Idle,
                is DraftTaskTrackingState.Processing,
                is DraftTaskTrackingState.Success,
                -> null

                is DraftTaskTrackingState.LongRunning ->
                    DraftLoadingNotice(
                        message = "생각보다 오래 걸리고 있어요. 계속 기다리거나 새로 만들 수 있어요.",
                        primaryAction = DraftLoadingAction("계속 기다리기", DraftLoadingUiIntent.ContinueWaiting),
                        secondaryAction = DraftLoadingAction("새로 만들기", DraftLoadingUiIntent.Discard),
                    )

                is DraftTaskTrackingState.RetryableError ->
                    DraftLoadingNotice(
                        message = "상태를 확인하지 못했어요. 잠시 후 다시 시도해주세요.",
                        primaryAction = DraftLoadingAction("다시 시도", DraftLoadingUiIntent.Retry),
                        secondaryAction = null,
                    )

                is DraftTaskTrackingState.Failed ->
                    DraftLoadingNotice(
                        message = "초안을 만들지 못했어요. 다시 시도해주세요.",
                        primaryAction = DraftLoadingAction("새로 만들기", DraftLoadingUiIntent.Discard),
                        secondaryAction = null,
                    )

                is DraftTaskTrackingState.Unavailable ->
                    DraftLoadingNotice(
                        message = "생성 요청을 찾지 못했어요. 새로 만들어주세요.",
                        primaryAction = DraftLoadingAction("새로 만들기", DraftLoadingUiIntent.Discard),
                        secondaryAction = null,
                    )
            }

        private companion object {
            /** 연출 경계가 1초 단위라 그보다 촘촘히 볼 이유가 없다. */
            val STAGE_TICK = 500.milliseconds
            val ALL_STAGES_DONE =
                DraftLoadingStage.entries.associateWith { DraftLoadingStageState.DONE }
        }
    }
