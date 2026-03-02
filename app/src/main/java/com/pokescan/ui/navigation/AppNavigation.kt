package com.pokescan.ui.navigation

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pokescan.ui.screens.camera.CameraScreen
import com.pokescan.ui.screens.carddetail.CardDetailScreen
import com.pokescan.ui.screens.collection.CollectionScreen
import com.pokescan.ui.screens.stats.StatsScreen

sealed class Screen(val route: String) {
    object Collection : Screen("collection")
    object Camera : Screen("camera")
    object Stats : Screen("stats")
    object CardDetail : Screen("card/{cardId}") {
        fun createRoute(cardId: String) = "card/$cardId"
    }
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Collection.route,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() },
    ) {
        composable(Screen.Collection.route) {
            CollectionScreen(
                onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                onNavigateToStats = { navController.navigate(Screen.Stats.route) },
                onCardClick = { cardId -> navController.navigate(Screen.CardDetail.createRoute(cardId)) },
            )
        }

        composable(Screen.Camera.route) {
            CameraScreen(
                onNavigateBack = { navController.popBackStack() },
                onCardFound = { cardId ->
                    navController.navigate(Screen.CardDetail.createRoute(cardId)) {
                        popUpTo(Screen.Camera.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.CardDetail.route,
            arguments = listOf(navArgument("cardId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId") ?: return@composable
            CardDetailScreen(
                cardId = cardId,
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
