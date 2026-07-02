package com.soma369.laimory.navigation

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.soma369.laimory.core.domain.message.UserMessage
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.feature.feature1.screen.Feature1Route
import com.soma369.laimory.feature.home.screen.HomeRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun LaimoryNavGraph(messages: Flow<UserMessage> = emptyFlow()) {
    val navController = rememberNavController()
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
            NavHost(
                navController = navController,
                startDestination = "home",
            ) {
                composable("home") {
                    HomeRoute(
                        innerPadding = innerPadding,
                        onNavigateToFeature1 = { navController.navigate("feature1") { launchSingleTop = true } },
                    )
                }
                composable("feature1") {
                    Feature1Route(
                        innerPadding = innerPadding,
                    )
                }
            }
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
