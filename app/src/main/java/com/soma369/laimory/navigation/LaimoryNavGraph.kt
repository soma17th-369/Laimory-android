package com.soma369.laimory.navigation

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.rememberNavBackStack
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.domain.navigation.HomePage
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Navigation 3 라우트 테이블 기반 앱 내비게이션의 진입 Composable.
 *
 * backStack(`NavBackStack<NavKey>`)을 app이 소유하고, 화면 분기는 [appRouteByPath]가 담당한다.
 * 단일 [Scaffold]/[SnackbarHost]가 chrome을 소유하고 [AppNavHost]가 content를 채운다.
 */
@Composable
fun LaimoryNavGraph(messages: Flow<UserMessage> = emptyFlow()) {
    val backStack = rememberNavBackStack(GenericNavKey(HomePage.PATH))
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(messages) {
        messages.collect { message ->
            snackbarHostState.showSnackbar(message.toText())
        }
    }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { innerPadding ->
            AppNavHost(
                backStack = backStack,
                innerPadding = innerPadding,
            )
        }
    }
}

/** 의미 수준 메시지를 실제 스낵바 문구로 매핑한다. (presentation 책임) */
private fun UserMessage.toText(): String =
    when (this) {
        UserMessage.SessionExpired -> "세션이 만료되었습니다. 다시 로그인해 주세요."
        UserMessage.UnsupportedFeature -> "현재 버전에서 지원하지 않는 기능입니다."
        UserMessage.TemporaryUnavailable -> "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
    }
