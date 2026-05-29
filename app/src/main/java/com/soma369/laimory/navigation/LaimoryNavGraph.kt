package com.soma369.laimory.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.soma369.laimory.feature.feature1.screen.Feature1Screen
import com.soma369.laimory.feature.home.screen.HomeScreen

@Composable
fun LaimoryNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home",
    ) {
        composable("home") {
            HomeScreen(onNavigateToFeature1 = { navController.navigate("feature1") { launchSingleTop = true } })
        }
        composable("feature1") {
            Feature1Screen(onBack = { navController.popBackStack() })
        }
    }
}
