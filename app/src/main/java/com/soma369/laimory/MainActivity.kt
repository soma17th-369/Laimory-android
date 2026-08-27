package com.soma369.laimory

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.soma369.laimory.core.data.helper.MessageHelperImpl
import com.soma369.laimory.core.data.helper.NavigationHelperImpl
import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.helper.GlobalLoadingHelper
import com.soma369.laimory.core.domain.helper.SocialLoginCallbackHandler
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.navigation.DraftLoadingPage
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.domain.navigation.TimelinePage
import com.soma369.laimory.core.domain.usecase.ObserveOnboardingCompletionUseCase
import com.soma369.laimory.core.domain.usecase.auth.ObserveAuthSessionUseCase
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.feature.home.draft.DraftConsentSessionStore
import com.soma369.laimory.navigation.LaimoryNavGraph
import com.soma369.laimory.push.DraftCompletionPushHandler
import com.soma369.laimory.push.DraftCompletionSignalParser
import com.soma369.laimory.ui.GlobalUiHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var messageHelper: MessageHelperImpl

    @Inject
    lateinit var globalLoadingHelper: GlobalLoadingHelper

    @Inject
    lateinit var navigationHelper: NavigationHelperImpl

    @Inject
    lateinit var socialLoginCallbackHandler: SocialLoginCallbackHandler

    @Inject
    lateinit var observeAuthSession: ObserveAuthSessionUseCase

    @Inject
    lateinit var observeOnboardingCompletion: ObserveOnboardingCompletionUseCase

    @Inject
    lateinit var draftCompletionPushHandler: DraftCompletionPushHandler

    @Inject
    lateinit var draftTaskCoordinator: DraftTaskCoordinator

    @Inject
    lateinit var draftConsentSessionStore: DraftConsentSessionStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumeSocialLoginCallback(intent)
        consumeDraftCompletionNotification(intent)
        // 로그인 직후 알림 권한을 바로 묻지 않는다. 무엇에 쓰는지 말하기 전에 뜨는 시스템
        // 다이얼로그는 거부되기 쉽고, 한 번 거부하면 다시 물을 기회가 사실상 없다.
        // 요청은 온보딩의 알림 장 CTA 가 맡는다.
        val authSessionStates = observeAuthSession()
        setContent {
            LaimoryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LaimoryNavGraph(
                            messages = messageHelper.messages,
                            onboardingCompletions = observeOnboardingCompletion(),
                            navigationFlow = navigationHelper.navigationFlow,
                            authSessionStates = authSessionStates,
                            pendingDraftCompletions = draftTaskCoordinator.pendingCompletion,
                            onDraftCompletionConsumed = draftTaskCoordinator::consumeCompletion,
                            onAuthRootReplaced = {
                                // 계정 경계 교체 시 이전 사용자의 대화 상자와 생성 시도 스냅샷을 함께 정리한다.
                                messageHelper.clearDialogs()
                                draftConsentSessionStore.clearAll()
                            },
                        )
                        GlobalUiHost(
                            messageHelper = messageHelper,
                            loadingHelper = globalLoadingHelper,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeSocialLoginCallback(intent)
        consumeDraftCompletionNotification(intent)
    }

    private fun consumeSocialLoginCallback(intent: Intent) {
        val callback = intent.dataString?.toSocialLoginCallbackOrNull() ?: return
        socialLoginCallbackHandler.handle(callback)
        // 구성 변경으로 Activity가 재생성돼도 같은 callback을 다시 제출하지 않는다.
        intent.data = null
        setIntent(intent)
    }

    private fun consumeDraftCompletionNotification(intent: Intent) {
        val taskId = intent.getStringExtra(DraftCompletionSignalParser.TASK_ID_KEY)
        val status = intent.getStringExtra(DraftCompletionSignalParser.STATUS_KEY)
        if (taskId == null && status == null) return
        intent.removeExtra(DraftCompletionSignalParser.TASK_ID_KEY)
        intent.removeExtra(DraftCompletionSignalParser.STATUS_KEY)
        setIntent(intent)
        lifecycleScope.launch {
            if (!draftCompletionPushHandler.onNotificationOpened(taskId, status)) return@launch
            // 알림 처리 화면으로 뒤로 돌아오지 않도록 홈을 루트로 세운다.
            navigationHelper.replaceRoot(HomePage)
            // 종료 상태에서 눌렀다면 활성 작업 복원이 아직 끝나지 않았을 수 있다. 바로 확인하면
            // 늘 Idle 이라 목적지를 정할 수 없으므로, 복원 결과나 완료 중 먼저 오는 쪽을 기다린다.
            val settled =
                withTimeoutOrNull(ACTIVE_TASK_RESTORE_TIMEOUT_MILLIS) {
                    combine(
                        draftTaskCoordinator.state,
                        draftTaskCoordinator.pendingCompletion,
                        ::Pair,
                    ).first { (state, completion) -> completion != null || state is DraftTaskTrackingState.WithTask }
                }
            // 끝내 복원되지 않으면 날짜를 추측하지 않고 홈에 머문다.
            val (trackingState, completion) = settled ?: return@launch
            // 완료를 우리가 집었으면 로딩 화면을 건너뛰고 서버 결과의 날짜로 바로 연다.
            if (completion != null && draftTaskCoordinator.consumeCompletion(completion.taskId)) {
                navigationHelper.navigateTo(TimelinePage(completion.recordDate))
                return@launch
            }
            // 내비게이션 호스트가 먼저 집었더라도 이미 끝난 작업 위에 로딩 화면을 얹지는 않는다.
            // 얹으면 완료가 사라진 뒤라 아무도 화면을 옮겨주지 않아 그대로 멈춘다.
            if (trackingState is DraftTaskTrackingState.Success) {
                navigationHelper.navigateTo(TimelinePage(trackingState.task.recordDate))
                return@launch
            }
            navigationHelper.navigateTo(DraftLoadingPage)
        }
    }

    private companion object {
        /** 알림을 눌러 앱이 처음 뜰 때 활성 작업 복원을 기다리는 한도. */
        const val ACTIVE_TASK_RESTORE_TIMEOUT_MILLIS = 3_000L
    }
}
