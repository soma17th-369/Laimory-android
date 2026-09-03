package com.soma369.laimory

import android.app.UiModeManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.getSystemService
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.soma369.laimory.core.data.helper.MessageHelperImpl
import com.soma369.laimory.core.data.helper.NavigationHelperImpl
import com.soma369.laimory.core.domain.coordinator.DraftTaskCoordinator
import com.soma369.laimory.core.domain.helper.GlobalLoadingHelper
import com.soma369.laimory.core.domain.helper.SocialLoginCallbackHandler
import com.soma369.laimory.core.domain.model.settings.AppThemeMode
import com.soma369.laimory.core.domain.model.timeline.DraftTaskTrackingState
import com.soma369.laimory.core.domain.navigation.DraftLoadingPage
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.domain.navigation.TimelinePage
import com.soma369.laimory.core.domain.usecase.ObserveOnboardingCompletionUseCase
import com.soma369.laimory.core.domain.usecase.auth.ObserveAuthSessionUseCase
import com.soma369.laimory.core.domain.usecase.settings.ObserveAppThemeModeUseCase
import com.soma369.laimory.core.domain.usecase.terms.ObserveTermsGateUseCase
import com.soma369.laimory.core.ui.theme.LaimoryTheme
import com.soma369.laimory.feature.home.draft.DraftConsentSessionStore
import com.soma369.laimory.navigation.LaimoryNavGraph
import com.soma369.laimory.push.DraftCompletionPushHandler
import com.soma369.laimory.push.DraftCompletionSignalParser
import com.soma369.laimory.ui.GlobalUiHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
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
    lateinit var observeTermsGate: ObserveTermsGateUseCase

    @Inject
    lateinit var draftCompletionPushHandler: DraftCompletionPushHandler

    @Inject
    lateinit var draftTaskCoordinator: DraftTaskCoordinator

    @Inject
    lateinit var draftConsentSessionStore: DraftConsentSessionStore

    @Inject
    lateinit var observeAppThemeMode: ObserveAppThemeModeUseCase

    /**
     * 저장된 화면 모드. `null` 은 아직 읽기 전이다.
     *
     * 값을 읽을 때까지 스플래시를 붙잡는다 — DataStore 는 비동기라 그냥 그리면 첫 프레임이
     * 기본값으로 나온 뒤 바뀌어, 다크를 골라 둔 사용자에게 흰 화면이 한 번 번쩍인다.
     */
    private val themeMode = MutableStateFlow<AppThemeMode?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { themeMode.value == null }
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            observeAppThemeMode().collect { mode ->
                themeMode.value = mode
                syncSystemNightMode(mode)
            }
        }
        consumeSocialLoginCallback(intent)
        consumeDraftCompletionNotification(intent)
        // 로그인 직후 알림 권한을 바로 묻지 않는다. 무엇에 쓰는지 말하기 전에 뜨는 시스템
        // 다이얼로그는 거부되기 쉽고, 한 번 거부하면 다시 물을 기회가 사실상 없다.
        // 요청은 온보딩의 알림 장 CTA 가 맡는다.
        val authSessionStates = observeAuthSession()
        setContent {
            val mode by themeMode.collectAsStateWithLifecycle()
            // 값을 읽기 전에는 그리지 않는다. 스플래시가 그동안 화면을 덮고 있다.
            val currentMode = mode ?: return@setContent
            LaimoryTheme(darkTheme = currentMode.isDark()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LaimoryNavGraph(
                            messages = messageHelper.messages,
                            onboardingCompletions = observeOnboardingCompletion(),
                            termsGateStates = observeTermsGate(),
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

    /**
     * 고른 화면 모드를 시스템에도 알린다.
     *
     * 콜드 스타트의 스플래시는 **우리 프로세스가 뜨기 전에** 시스템이 액티비티 테마로 그린다.
     * 그래서 앱 안에서 무엇을 하든 그 한 프레임의 색은 바꿀 수 없다 — 시스템이 미리 알고 있어야
     * 한다. API 31+ 는 앱별 야간 모드를 시스템에 저장할 수 있어, 여기에 값을 넘겨 두면 다음
     * 실행부터 스플래시가 고른 테마로 뜬다.
     *
     * 28~30 에는 같은 저장소가 없어 스플래시가 OS 설정을 따른다. 앱 화면 자체는 어느 버전에서도
     * 반대 테마로 그려지지 않는다 — 저장값을 읽을 때까지 스플래시가 화면을 덮고 있기 때문이다.
     *
     * 앱의 색을 정하는 것은 여전히 [LaimoryTheme] 에 넘기는 값이다. 이 호출은 시스템이 그리는
     * 자리(스플래시·작업 전환 미리보기)의 색만 맞춘다.
     */
    private fun syncSystemNightMode(mode: AppThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val uiModeManager = getSystemService<UiModeManager>() ?: return
        val nightMode =
            when (mode) {
                AppThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                AppThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
                AppThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            }
        // 같은 값이면 시스템이 무시한다. 다를 때만 구성 변경이 한 번 일어난다.
        uiModeManager.setApplicationNightMode(nightMode)
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

/**
 * OS 설정을 따를지, 고른 값을 따를지.
 *
 * `SYSTEM` 만 `isSystemInDarkTheme()` 을 읽으므로 실행 중 OS 가 바뀌면 그때만 함께 바뀐다.
 */
@androidx.compose.runtime.Composable
private fun AppThemeMode.isDark(): Boolean =
    when (this) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
