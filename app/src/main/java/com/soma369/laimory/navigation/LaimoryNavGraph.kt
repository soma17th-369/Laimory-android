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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.model.timeline.DraftTaskCompletion
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.domain.navigation.LoginPage
import com.soma369.laimory.core.domain.navigation.NavSignal
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.domain.navigation.TimelinePage
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.push.DraftCompletionNotificationChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

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
    draftCompletions: Flow<DraftTaskCompletion> = emptyFlow(),
    onAuthRootReplaced: () -> Unit = {},
) {
    val sessionState by authSessionStates.collectAsStateWithLifecycle(initialValue = AuthSessionState.Loading)
    val rootPage = sessionState.rootPage()
    if (rootPage == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val context = LocalContext.current
    val backStack = rememberNavBackStack(GenericNavKey(rootPage.toRoute().path))
    val snackbarHostState = remember { SnackbarHostState() }

    // 바텀바 노출·선택 상태는 별도 상태 없이 backStack top 의 path 에서 파생한다.
    val currentPath = (backStack.lastOrNull() as? GenericNavKey)?.path

    LaunchedEffect(messages) {
        messages.collect { message ->
            snackbarHostState.showSnackbar(message.toText())
        }
    }

    // 초안 완료 분기는 여기 한 곳에서만 한다. 완료는 어느 화면에서든 올 수 있고, 상태가 아니라
    // 일회성 신호를 쓰므로 화면마다 구독하면 회전·복원 때 중복 실행된다.
    LaunchedEffect(draftCompletions) {
        draftCompletions.collect { completion ->
            val timelineRoute = TimelinePage(completion.recordDate).toRoute()
            // 결과를 확인했으므로 백그라운드에서 온 알림은 더 알릴 것이 없다.
            DraftCompletionNotificationChannel.dismissAll(context)
            if (backStack.isShowingDraftLoading()) {
                // 마지막 줄이 완료로 바뀌는 것을 잠깐 보여주고 넘어간다. 바로 바꾸면 `분석 중...`을
                // 보다가 예고 없이 화면이 튄다.
                delay(COMPLETION_REVEAL_MILLIS)
            }
            if (backStack.isShowingDraftLoading()) {
                // 다 만든 화면으로 되돌아갈 이유가 없어 최상단을 갈아 끼운다.
                backStack.replaceTopWith(timelineRoute)
                return@collect
            }
            val result =
                snackbarHostState.showSnackbar(
                    message = "초안이 완성됐어요",
                    actionLabel = "보기",
                )
            // 스낵바가 닫혀도 완료 상태는 남으므로 홈의 `초안 보기`로 나중에 열 수 있다.
            if (result == SnackbarResult.ActionPerformed) backStack.navigateTo(timelineRoute)
        }
    }

    LaunchedEffect(rootPage) {
        backStack.syncAuthRoot(rootPage, onRootReplaced = onAuthRootReplaced)
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

internal fun AuthSessionState.rootPage(): Page? =
    when (this) {
        AuthSessionState.Loading -> null
        AuthSessionState.Authenticated -> HomePage
        AuthSessionState.Unauthenticated -> LoginPage
    }

/**
 * 인증 여부가 바뀐 경우에만 Login/Home 경계를 교체하고, 같은 경계의 복원된 백스택은 보존한다.
 *
 * [onRootReplaced]는 실제 경계 교체 순간에만 호출된다 — 구성 변경으로 같은 Root가 다시
 * 구성될 때는 호출되지 않아 활성 Dialog가 유지되고, 교체 뒤에는 오래된 요청이 정리된다.
 */
internal fun NavBackStack<NavKey>.syncAuthRoot(
    targetRoot: Page,
    onRootReplaced: () -> Unit = {},
) {
    val currentRootPath = (firstOrNull() as? GenericNavKey)?.path
    val isLoginRoot = currentRootPath == LoginPage.PATH
    val shouldBeLoginRoot = targetRoot == LoginPage
    if (isLoginRoot != shouldBeLoginRoot) {
        onRootReplaced()
        replaceRoot(targetRoot.toRoute())
    }
}

/** 의미 수준 메시지를 실제 스낵바 문구로 매핑한다. (presentation 책임) */
private fun UserMessage.toText(): String =
    when (this) {
        UserMessage.SessionExpired -> "세션이 만료되었습니다. 다시 로그인해 주세요."
        UserMessage.UnsupportedFeature -> "현재 버전에서 지원하지 않는 기능입니다."
        UserMessage.TemporaryUnavailable -> "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
        UserMessage.DailyRecordSaved -> "하루 기록 작성을 완료했어요."
    }
