package com.soma369.laimory.navigation

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.soma369.laimory.core.ui.LocalSnackbarHostState
import com.soma369.laimory.feature.feature1.screen.Feature1Route
import com.soma369.laimory.feature.home.screen.HomeRoute

@Composable
fun LaimoryNavGraph() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

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
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
