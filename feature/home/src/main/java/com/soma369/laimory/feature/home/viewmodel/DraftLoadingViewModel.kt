package com.soma369.laimory.feature.home.viewmodel

import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.helper.NavigationHelper
import com.soma369.laimory.core.domain.model.collection.SourceItemRetentionConfig
import com.soma369.laimory.core.domain.model.timeline.ActiveDraftTask
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.ui.base.BaseMviViewModel
import com.soma369.laimory.core.ui.base.UiSideEffect
import com.soma369.laimory.feature.home.draft.DraftLoadingSession
import com.soma369.laimory.feature.home.draft.DraftLoadingSessionStore
import com.soma369.laimory.feature.home.draft.DraftSubmission
import com.soma369.laimory.feature.home.draft.DraftSubmissionStore
import com.soma369.laimory.feature.home.draft.DraftSubmitFailureKind
import com.soma369.laimory.feature.home.draft.DraftSubmitFailureMessages
import com.soma369.laimory.feature.home.loading.DraftLoadingAction
import com.soma369.laimory.feature.home.loading.DraftLoadingNotice
import com.soma369.laimory.feature.home.loading.DraftLoadingStage
import com.soma369.laimory.feature.home.loading.DraftLoadingStageMath
import com.soma369.laimory.feature.home.loading.DraftLoadingStageState
import com.soma369.laimory.feature.home.loading.DraftLoadingUiIntent
import com.soma369.laimory.feature.home.loading.DraftLoadingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
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
        private val submissionStore: DraftSubmissionStore,
        private val navigationHelper: NavigationHelper,
        private val clock: Clock,
        retentionConfig: SourceItemRetentionConfig,
    ) : BaseMviViewModel<DraftLoadingUiState, DraftLoadingUiIntent, UiSideEffect>(
            DraftLoadingUiState(retentionDays = retentionConfig.retentionDays),
        ) {
        /** 이미 돌아간 실패. 같은 실패가 다시 흘러와도 두 번 돌아가지 않는다. */
        private var leftOnFailure: DraftSubmission.Failed? = null

        init {
            observeTask()
            tickStages()
        }

        override suspend fun handleIntent(intent: DraftLoadingUiIntent) {
            when (intent) {
                is DraftLoadingUiIntent.ChangeVisibility -> submissionStore.setLoadingShown(intent.shown)
                // 실패 안내를 띄운 채 뒤로 나가는 것도 안내를 본 것이다.
                DraftLoadingUiIntent.NavigateBack -> {
                    submissionStore.markShownOnLoading()
                    navigationHelper.navigateToBack()
                }
                DraftLoadingUiIntent.LeaveAfterSubmitFailure -> {
                    submissionStore.markShownOnLoading()
                    navigationHelper.navigateToBack()
                }
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
                combine(coordinator.state, loadingSessionStore.session, submissionStore.submission) { tracking, session, submission ->
                    Triple(tracking, session, submission)
                }.collect { (tracking, session, submission) ->
                    // 작업 번호를 받기 전에는 요청 상태가 화면을 정한다. 그동안 coordinator 에는 이전 작업이 남아 있을 수
                    // 있어(다른 날짜의 완료 등) 그것을 보면 엉뚱한 완료 · 안내가 뜬다.
                    if (submission !is DraftSubmission.Idle) {
                        showSubmission(submission, session)
                        if (submission is DraftSubmission.Failed) leaveOnNetworkFailure(submission)
                        return@collect
                    }
                    val task = (tracking as? DraftTaskTrackingState.WithTask)?.task
                    val matched = task?.taskId?.let { loadingSessionStore.sessionFor(it) } ?: session
                    val isCompleted = tracking is DraftTaskTrackingState.Success
                    updateState {
                        copy(
                            recordDate = task?.recordDate ?: matched?.recordDate ?: recordDate,
                            // 스냅샷이 없어져도 화면은 마지막 값을 유지한다. terminal 에서 스냅샷을
                            // 지우는데, 그때 빈 값으로 덮으면 완료를 보여주는 동안 사진이 사라지고
                            // `0장 완료`가 뜬다.
                            photoUris = matched?.photoUris ?: photoUris,
                            photoCount = matched?.photoCount ?: photoCount,
                            calendarCount = matched?.calendarCount ?: calendarCount,
                            stayCount = matched?.stayCount ?: stayCount,
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

        private fun showSubmission(
            submission: DraftSubmission,
            session: DraftLoadingSession?,
        ) {
            val recordDate =
                when (submission) {
                    is DraftSubmission.Submitting -> submission.recordDate
                    is DraftSubmission.Failed -> submission.recordDate
                    DraftSubmission.Idle -> null
                }
            updateState {
                copy(
                    recordDate = recordDate ?: this.recordDate,
                    photoUris = session?.photoUris ?: photoUris,
                    photoCount = session?.photoCount ?: photoCount,
                    calendarCount = session?.calendarCount ?: calendarCount,
                    stayCount = session?.stayCount ?: stayCount,
                    notice = (submission as? DraftSubmission.Failed)?.toNotice(),
                )
            }
        }

        /**
         * 연결이 없어 요청을 못 보냈으면 안내 없이 곧바로 홈으로 돌아간다. 알림은 홈이 스낵바로 한다 — 만들기 전에 막을
         * 때와 같은 모습이고, 고칠 것은 연결뿐이라 이 화면에서 읽을 사유가 없다.
         *
         * 로딩 화면이 떠 있을 때만 돌아간다. 이 ViewModel 은 Activity 범위라 화면을 떠난 뒤에도 실패를 받는데, 그때
         * 뒤로 가면 홈을 닫는다.
         */
        private fun leaveOnNetworkFailure(failed: DraftSubmission.Failed) {
            if (failed.kind != DraftSubmitFailureKind.NETWORK) return
            if (failed === leftOnFailure || !submissionStore.isLoadingShown.value) return
            leftOnFailure = failed
            navigationHelper.navigateToBack()
        }

        /**
         * 요청 실패 안내. 연결 없음은 안내하지 않는다([leaveOnNetworkFailure]). 자동으로 돌아가지 않는다 — 사유를 읽기 전에 화면이 바뀌면 왜 실패했는지 모른다. 버튼 문구는
         * 돌아간 뒤 홈이 할 일을 말한다(사진 시트를 다시 연다 · 약관 화면으로 간다).
         */
        private fun DraftSubmission.Failed.toNotice(): DraftLoadingNotice? {
            val home = DraftLoadingAction("홈으로", DraftLoadingUiIntent.LeaveAfterSubmitFailure)
            val repick = DraftLoadingAction("사진 다시 고르기", DraftLoadingUiIntent.LeaveAfterSubmitFailure)
            return when (kind) {
                DraftSubmitFailureKind.TERMS_REQUIRED ->
                    DraftLoadingNotice(
                        message = DraftSubmitFailureMessages.TERMS_REQUIRED,
                        primaryAction = DraftLoadingAction("약관 확인하기", DraftLoadingUiIntent.LeaveAfterSubmitFailure),
                        secondaryAction = null,
                    )
                DraftSubmitFailureKind.NO_NEW_ITEMS -> DraftLoadingNotice(DraftSubmitFailureMessages.NO_NEW_ITEMS, home, null)
                DraftSubmitFailureKind.PHOTO_ACCESS -> DraftLoadingNotice(DraftSubmitFailureMessages.PHOTO_ACCESS, repick, null)
                DraftSubmitFailureKind.PHOTO_LIMIT ->
                    DraftLoadingNotice("${error.message}\n${DraftSubmitFailureMessages.PHOTO_LIMIT_SUFFIX}", repick, null)
                DraftSubmitFailureKind.TIMEOUT -> DraftLoadingNotice(DraftSubmitFailureMessages.TIMEOUT, home, null)
                DraftSubmitFailureKind.NETWORK -> null
                DraftSubmitFailureKind.OTHER -> DraftLoadingNotice(DraftSubmitFailureMessages.OTHER, home, null)
            }
        }

        /**
         * 경과 시간에 따라 앞 세 줄을 차례로 완료로 바꾼다. **추적하는 작업이 바뀌면 다시 돈다.**
         *
         * 이 ViewModel 은 Activity 범위라 로딩 화면을 다시 열어도 새로 만들어지지 않는다 — `NavDisplay` 에
         * 엔트리 범위 ViewModel 을 붙이지 않았다. 연출을 `init` 에서 한 번 돌리고 작업이 끝날 때 멈추면,
         * 다음 작업의 로딩 화면은 이전 작업의 마지막 모습에서 굳는다(직전이 성공이면 네 줄 모두 완료).
         * 그래서 작업마다 새로 시작한다. 경과는 새 작업의 `requestedAt` 으로 세므로 처음 줄부터 진행한다.
         *
         * 같은 작업으로 재진입하면 이어서 간다 — 화면 표시를 위해 서버를 다시 부르지 않고, 작업의
         * `requestedAt` 으로 경과를 계산하므로 연출이 처음부터 다시 시작하지 않는다.
         */
        private fun tickStages() {
            safeLaunch {
                combine(
                    coordinator.state.map { (it as? DraftTaskTrackingState.WithTask)?.task?.taskId },
                    submissionStore.submission.map { (it as? DraftSubmission.Submitting)?.startedAt },
                ) { taskId, submittedAt -> taskId to submittedAt }
                    .distinctUntilChanged()
                    .collectLatest { (taskId, _) ->
                        while (true) {
                            val submission = submissionStore.submission.value
                            val tracking = coordinator.state.value
                            val task = (tracking as? DraftTaskTrackingState.WithTask)?.task
                            // 요청을 보내는 동안은 아직 작업이 없다. `만들기` 를 누른 시각부터 센다 — 사진 업로드가 실제로
                            // 도는 구간이라 첫 줄(사진)과 맞는다.
                            val submitting = submission as? DraftSubmission.Submitting
                            val elapsed =
                                if (submitting != null) {
                                    Duration.between(submitting.startedAt, clock.instant()).toKotlinDuration()
                                } else {
                                    elapsedOf(tracking, task)
                                }
                            val isCompleted = submission is DraftSubmission.Idle && tracking is DraftTaskTrackingState.Success
                            updateState {
                                copy(
                                    stageStates =
                                        DraftLoadingStage.entries.associateWith { stage ->
                                            DraftLoadingStageMath.stateOf(stage, elapsed, isCompleted)
                                        },
                                )
                            }
                            // 요청이 실패했거나, 끝난 작업이나 추적할 작업이 없으면 더 움직일 연출이 없다. 다음이 오면 다시 돈다.
                            if (submission is DraftSubmission.Failed) break
                            if (submitting == null && (taskId == null || tracking.isTerminal())) break
                            delay(STAGE_TICK)
                        }
                    }
            }
        }

        private fun DraftTaskTrackingState.isTerminal(): Boolean =
            this is DraftTaskTrackingState.Success ||
                this is DraftTaskTrackingState.Failed ||
                this is DraftTaskTrackingState.Unavailable

        /**
         * 연출에 쓸 경과 시간.
         *
         * 기기 시각만 쓰면 시각이 바뀌거나 어긋났을 때 단계가 뒤로 간다. 서버가 알려준 경과 시간을
         * 하한으로 두어, 복원 뒤에도 이미 지나간 단계가 되돌아오지 않게 한다.
         */
        private fun elapsedOf(
            tracking: DraftTaskTrackingState,
            task: ActiveDraftTask?,
        ): kotlin.time.Duration {
            val local =
                task
                    ?.let { Duration.between(it.requestedAt, clock.instant()) }
                    ?.toKotlinDuration()
                    ?: kotlin.time.Duration.ZERO
            // 요청 중에 이미 흐른 연출이 작업 번호를 받은 뒤 되돌아가지 않게, `만들기` 를 누른 시각도 하한으로 둔다.
            val sinceSubmit =
                task
                    ?.let { loadingSessionStore.sessionFor(it.taskId)?.submittedAt }
                    ?.let { Duration.between(it, clock.instant()).toKotlinDuration() }
                    ?: kotlin.time.Duration.ZERO
            val server =
                when (tracking) {
                    is DraftTaskTrackingState.Processing -> tracking.elapsedSeconds
                    is DraftTaskTrackingState.LongRunning -> tracking.elapsedSeconds
                    else -> null
                }?.seconds ?: kotlin.time.Duration.ZERO
            return maxOf(local, server, sinceSubmit).coerceAtLeast(kotlin.time.Duration.ZERO)
        }

        private fun DraftTaskTrackingState.toNotice(): DraftLoadingNotice? =
            when (this) {
                DraftTaskTrackingState.Idle,
                is DraftTaskTrackingState.Processing,
                is DraftTaskTrackingState.Success,
                -> null

                is DraftTaskTrackingState.LongRunning ->
                    DraftLoadingNotice(
                        message = "생각보다 오래 걸리고 있어요. 계속 기다리거나 다시 만들 수 있어요.",
                        primaryAction = DraftLoadingAction("계속 기다리기", DraftLoadingUiIntent.ContinueWaiting),
                        secondaryAction = DraftLoadingAction("다시 만들기", DraftLoadingUiIntent.Discard),
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
                        primaryAction = DraftLoadingAction("다시 만들기", DraftLoadingUiIntent.Discard),
                        secondaryAction = null,
                    )

                is DraftTaskTrackingState.Unavailable ->
                    DraftLoadingNotice(
                        message = "생성 요청을 찾지 못했어요. 다시 만들어주세요.",
                        primaryAction = DraftLoadingAction("다시 만들기", DraftLoadingUiIntent.Discard),
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
