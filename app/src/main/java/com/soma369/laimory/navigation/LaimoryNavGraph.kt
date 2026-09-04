package com.soma369.laimory.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.terms.TermsGateState
import com.soma369.laimory.core.domain.model.timeline.DraftTaskCompletion
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.domain.navigation.LoginPage
import com.soma369.laimory.core.domain.navigation.NavSignal
import com.soma369.laimory.core.domain.navigation.OnboardingPage
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.navigation.TermsPage
import com.soma369.laimory.core.domain.navigation.TimelinePage
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.push.DraftCompletionNotificationChannel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

/**
 * Navigation 3 라우트 테이블 기반 앱 내비게이션의 진입 Composable.
 *
 * backStack(`NavBackStack<NavKey>`)을 app이 소유하고, 화면 분기는 [appRouteByPath]가 담당한다.
 * 단일 [Scaffold]가 chrome(스낵바·바텀바)을 소유하고 [AppNavHost]가 content를 채운다.
 */
@Composable
fun LaimoryNavGraph(
    messages: Flow<UserMessage> = emptyFlow(),
    navigationFlow: Flow<NavSignal> = emptyFlow(),
    authSessionStates: Flow<AuthSessionState> = flowOf(AuthSessionState.Authenticated),
    onboardingCompletions: Flow<Boolean?> = flowOf(true),
    termsGateStates: Flow<TermsGateState> = flowOf(TermsGateState.Satisfied),
    pendingDraftCompletions: StateFlow<DraftTaskCompletion?> = MutableStateFlow(null),
    onDraftCompletionConsumed: suspend (String) -> Boolean = { false },
    onAuthRootReplaced: () -> Unit = {},
) {
    val sessionState by authSessionStates.collectAsStateWithLifecycle(initialValue = AuthSessionState.Loading)
    // 아직 읽기 전이면 null 이다. 온보딩 상태를 모르는 채로 Home 을 먼저 그리면, 온보딩이 필요한
    // 사용자에게 홈이 한 프레임 번쩍인 뒤 화면이 갈린다.
    val onboardingCompleted by onboardingCompletions.collectAsStateWithLifecycle(initialValue = null)
    // 약관 판정도 같은 이유로 정해지기 전에는 루트를 고르지 않는다. 이용약관에 동의하지 않으면
    // 서버가 인증 API 대부분을 막으므로, 모른 채 홈을 그리면 오류만 뜨는 화면이 먼저 보인다.
    val termsGate by termsGateStates.collectAsStateWithLifecycle(initialValue = TermsGateState.Unknown)
    val rootPage = rootPage(sessionState, termsGate, onboardingCompleted)
    if (rootPage == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val context = LocalContext.current
    val backStack = rememberNavBackStack(GenericNavKey(rootPage.toRoute().path))
    // 이 백스택이 마지막으로 세운 경계 루트. 백스택과 함께 저장·복원돼야 구성 변경을 경계 변화로
    // 오인하지 않는다. 첫 구성에서는 백스택도 같은 루트로 만들어지므로 둘이 서로 맞는다.
    var appliedRootPath by rememberSaveable { mutableStateOf(rootPage.toRoute().path) }
    val snackbarHostState = remember { SnackbarHostState() }

    // 바텀바 노출·선택 상태는 별도 상태 없이 backStack top 의 path 에서 파생한다.
    val currentPath = (backStack.lastOrNull() as? GenericNavKey)?.path

    LaunchedEffect(messages) {
        messages.collect { message ->
            snackbarHostState.showSnackbar(message.toText())
        }
    }

    // 초안 완료 분기는 여기 한 곳에서만 한다. 완료는 어느 화면에서든 올 수 있어 화면마다 구독하면
    // 회전·복원 때 중복 실행된다.
    //
    // 완료와 백스택 최상단을 함께 본다. 목적지는 둘의 함수이고, 완료가 먼저 와도(알림 진입은 로딩
    // 화면을 얹기까지 시간이 걸린다) 최상단이 바뀌면 collectLatest 가 판단을 다시 시킨다. 소비는
    // 화면을 실제로 옮기는 순간에만 하므로, 그 전까지는 완료가 살아 있어 다시 판단할 수 있다.
    // 메서드 참조는 리컴포지션마다 새 객체라 키로 쓰면 수집이 계속 재시작한다. 최신 람다만 따라간다.
    val currentOnConsumed by rememberUpdatedState(onDraftCompletionConsumed)
    LaunchedEffect(pendingDraftCompletions) {
        combine(
            pendingDraftCompletions,
            snapshotFlow { backStack.isShowingDraftLoading() },
            ::Pair,
        ).collectLatest { (completion, isShowingLoading) ->
            if (completion == null) return@collectLatest
            val timelineRoute = TimelinePage(completion.recordDate).toRoute()
            // 결과를 확인했으므로 백그라운드에서 온 알림은 더 알릴 것이 없다.
            DraftCompletionNotificationChannel.dismissAll(context)
            if (isShowingLoading) {
                // 마지막 줄이 완료로 바뀌는 것을 잠깐 보여주고 넘어간다. 바로 바꾸면 `분석 중...`을
                // 보다가 예고 없이 화면이 튄다.
                delay(COMPLETION_REVEAL_MILLIS)
                // 소비하면 완료가 비어 이 블록이 취소되므로, 이동까지 한 묶음으로 끝낸다.
                withContext(NonCancellable) {
                    // 다 만든 화면으로 되돌아갈 이유가 없어 최상단을 갈아 끼운다.
                    if (currentOnConsumed(completion.taskId)) backStack.replaceTopWith(timelineRoute)
                }
                return@collectLatest
            }
            val result =
                snackbarHostState.showSnackbar(
                    message = "초안이 완성됐어요",
                    actionLabel = "보기",
                )
            // 스낵바가 떠 있는 동안 로딩 화면이 올라오면 이 블록이 취소되고 위 분기로 다시 간다.
            // 그래서 소비는 스낵바가 끝난 뒤에 한다.
            withContext(NonCancellable) {
                if (!currentOnConsumed(completion.taskId)) return@withContext
                // 스낵바가 닫혀도 완료 상태는 남으므로 홈의 `초안 보기`로 나중에 열 수 있다.
                if (result == SnackbarResult.ActionPerformed) backStack.navigateTo(timelineRoute)
            }
        }
    }

    LaunchedEffect(rootPage) {
        backStack.syncRoot(rootPage, appliedRootPath, onRootReplaced = onAuthRootReplaced)
        appliedRootPath = rootPage.toRoute().path
    }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                // 탭 루트에서만 노출한다. push 된 일반 화면(수집 등)에서는 숨긴다.
                if (currentPath != null && appRouteByPath[currentPath]?.isBottomTab == true) {
                    AppBottomBar(
                        currentPath = currentPath,
                        onTabSelect = { route -> backStack.switchTab(route.path) },
                    )
                }
            },
        ) { innerPadding ->
            AppNavHost(
                backStack = backStack,
                innerPadding = innerPadding,
                navigationFlow = navigationFlow,
            )
        }
    }
}

/** 완료 표시를 보여주는 시간. 넘어가기 전에 `완료`로 바뀌는 것을 알아볼 만큼만 둔다. */
private const val COMPLETION_REVEAL_MILLIS = 800L

/**
 * 인증과 온보딩을 함께 보고 앱 루트를 하나로 정한다. `null` 이면 아직 정할 수 없다는 뜻이다.
 *
 * 완료 여부는 계정 단위라 서버가 정본이다. `null` 은 아직 조회 전이라는 뜻이고, 그동안에는
 * Home 도 온보딩도 열지 않는다 — 모르는 채로 하나를 고르면 반대였을 때 화면이 한 번 번쩍인 뒤
 * 갈린다.
 *
 * 로그인하지 않았으면 기다리지 않는다 — 로그인 화면은 온보딩과 무관해서, 기다리면 로그인이
 * 늦게 뜨기만 한다.
 */
internal fun rootPage(
    session: AuthSessionState,
    termsGate: TermsGateState,
    onboardingCompleted: Boolean?,
): Page? =
    when (session) {
        AuthSessionState.Loading -> null
        AuthSessionState.Unauthenticated -> LoginPage
        AuthSessionState.Authenticated ->
            when (termsGate) {
                TermsGateState.Unknown -> null
                // 조회 실패도 약관 화면이 받는다. 그 화면이 다시 시도와 로그아웃을 함께 갖는다.
                is TermsGateState.Required, TermsGateState.Failed -> TermsPage
                TermsGateState.Satisfied ->
                    when (onboardingCompleted) {
                        null -> null
                        true -> HomePage
                        false -> OnboardingPage
                    }
            }
    }

/**
 * 경계가 실제로 바뀐 경우에만 루트를 교체하고, 같은 경계의 복원된 백스택은 보존한다.
 *
 * [appliedRootPath]는 이 백스택이 마지막으로 세운 경계 루트의 경로다. 백스택 바닥으로 판별하지
 * 않는다 — 탭 전환이 바닥을 탭 루트로 갈아끼우므로([switchTab]) 홈이 아닌 탭에 있으면 바닥은
 * `/settings`다. 구성 변경(화면 모드·회전·글꼴 크기)으로 이 함수가 다시 돌 때 그것을 경계 변화로
 * 읽으면, 보고 있던 화면이 홈 한 건으로 날아가고 [onRootReplaced]까지 헛돌아 활성 Dialog가 사라진다.
 *
 * 루트가 셋(Login·Onboarding·Home)이라 "로그인 화면인가" 같은 불리언으로는 가를 수 없다. 경로를
 * 직접 비교하면 루트가 더 늘어도 그대로 쓴다.
 *
 * [onRootReplaced]는 경계가 바뀌는 순간에만 호출된다. 화면 쪽이 흐름보다 먼저 루트를 갈아 끼운
 * 뒤(로그아웃·로그인 성공)라 스택이 이미 목표 루트여도, 경계가 바뀐 것은 사실이므로 호출된다 —
 * 이전 계정의 Dialog와 동의 스냅샷을 정리할 자리가 거기 한 곳뿐이다.
 */
internal fun NavBackStack<NavKey>.syncRoot(
    targetRoot: Page,
    appliedRootPath: String,
    onRootReplaced: () -> Unit = {},
) {
    if (appliedRootPath == targetRoot.toRoute().path) return
    onRootReplaced()
    replaceRoot(targetRoot.toRoute())
}

/** 의미 수준 메시지를 실제 스낵바 문구로 매핑한다. (presentation 책임) */
private fun UserMessage.toText(): String =
    when (this) {
        UserMessage.SessionExpired -> "세션이 만료되었습니다. 다시 로그인해 주세요."
        UserMessage.UnsupportedFeature -> "현재 버전에서 지원하지 않는 기능입니다."
        UserMessage.TemporaryUnavailable -> "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
        UserMessage.DailyRecordSaved -> "하루 기록 작성을 완료했어요."
        UserMessage.AccountWithdrawalAccepted -> "계정 삭제를 접수했어요. 삭제 처리에는 시간이 걸릴 수 있어요."
        // 서버가 만료 세션과 이미 탈퇴한 회원을 같은 401 로 합치므로 완료를 단정하지 않는다.
        UserMessage.AccountWithdrawalUnverified -> "로그인이 만료되어 삭제 결과를 확인하지 못했어요. 다시 로그인해 확인해 주세요."
    }
