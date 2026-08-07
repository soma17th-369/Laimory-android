package com.soma369.laimory.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.soma369.laimory.core.domain.message.ActiveDialog
import com.soma369.laimory.core.domain.message.DialogResult
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.model.auth.AuthSessionState
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.domain.navigation.LoginPage
import com.soma369.laimory.core.domain.navigation.NavSignal
import com.soma369.laimory.core.domain.navigation.Page
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.ui.GlobalDialogHost
import com.soma369.laimory.ui.GlobalLoadingOverlay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    activeDialogs: StateFlow<ActiveDialog?> = MutableStateFlow(null),
    onDialogResult: (requestId: Long, result: DialogResult) -> Unit = { _, _ -> },
    loadingKeys: StateFlow<Set<String>> = MutableStateFlow(emptySet()),
    onAuthRootReplaced: () -> Unit = {},
    navigationFlow: Flow<NavSignal> = emptyFlow(),
    authSessionStates: Flow<AuthSessionState> = flowOf(AuthSessionState.Authenticated),
) {
    val sessionState by authSessionStates.collectAsStateWithLifecycle(initialValue = AuthSessionState.Loading)
    val rootPage = sessionState.rootPage()
    if (rootPage == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val backStack = rememberNavBackStack(GenericNavKey(rootPage.toRoute().path))
    val snackbarHostState = remember { SnackbarHostState() }

    // 바텀바 노출·선택 상태는 별도 상태 없이 backStack top 의 path 에서 파생한다.
    val currentPath = (backStack.lastOrNull() as? GenericNavKey)?.path

    LaunchedEffect(messages) {
        messages.collect { message ->
            snackbarHostState.showSnackbar(message.toText())
        }
    }

    LaunchedEffect(rootPage) {
        backStack.syncAuthRoot(rootPage, onRootReplaced = onAuthRootReplaced)
    }

    val activeDialog by activeDialogs.collectAsStateWithLifecycle()
    val activeLoadingKeys by loadingKeys.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Box(modifier = Modifier.fillMaxSize()) {
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
            GlobalLoadingOverlay(isVisible = activeLoadingKeys.isNotEmpty())
        }
        GlobalDialogHost(activeDialog = activeDialog, onResult = onDialogResult)
    }
}

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
    }
