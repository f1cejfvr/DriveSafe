package com.rmas.drivesafe.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rmas.drivesafe.ui.auth.LoginScreen
import com.rmas.drivesafe.ui.auth.RegisterScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Map : Screen("map")
    object ObjectList : Screen("object_list")
    object Leaderboard : Screen("leaderboard")
    object Profile : Screen("profile")
    object AddObject : Screen("add_object")
    object ObjectDetail : Screen("object_detail/{objectId}") {
        fun createRoute(objectId: String) = "object_detail/$objectId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController)
        }
        composable(Screen.Map.route) {
            // MapScreen(navController)
        }
        composable(Screen.ObjectList.route) {
            // ObjectListScreen(navController)
        }
        composable(Screen.Leaderboard.route) {
            // LeaderboardScreen(navController)
        }
        composable(Screen.Profile.route) {
            // ProfileScreen(navController)
        }
        composable(Screen.AddObject.route) {
            // AddObjectScreen(navController)
        }
        composable(Screen.ObjectDetail.route) {
            // ObjectDetailScreen(navController)
        }
    }
}