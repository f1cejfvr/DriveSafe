package com.rmas.drivesafe.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rmas.drivesafe.ui.auth.LoginScreen
import com.rmas.drivesafe.ui.auth.RegisterScreen
import com.rmas.drivesafe.ui.map.MapScreen
import com.rmas.drivesafe.ui.objects.AddObjectScreen
import com.rmas.drivesafe.ui.objects.ObjectDetailScreen
import com.rmas.drivesafe.ui.objects.ObjectListScreen
import com.rmas.drivesafe.ui.leaderboard.LeaderboardScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Map : Screen("map")
    object ObjectList : Screen("object_list")
    object Leaderboard : Screen("leaderboard")
    object Profile : Screen("profile")
    object AddObject : Screen("add_object?lat={lat}&lng={lng}") {
        fun createRoute(lat: Double, lng: Double) = "add_object?lat=$lat&lng=$lng"
    }
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
            MapScreen(navController)
        }
        composable(Screen.ObjectList.route) {
            ObjectListScreen(navController)
        }
        composable(Screen.Leaderboard.route) {
            LeaderboardScreen(navController)
        }
        composable(Screen.Profile.route) {
            // ProfileScreen(navController)
        }
        composable(
            route = Screen.AddObject.route,
            arguments = listOf(
                navArgument("lat") {
                    type = NavType.FloatType
                    defaultValue = 43.3209f
                },
                navArgument("lng") {
                    type = NavType.FloatType
                    defaultValue = 21.8954f
                }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 43.3209
            val lng = backStackEntry.arguments?.getFloat("lng")?.toDouble() ?: 21.8954
            AddObjectScreen(navController, latitude = lat, longitude = lng)
        }
        composable(
            route = Screen.ObjectDetail.route,
            arguments = listOf(
                navArgument("objectId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val objectId = backStackEntry.arguments?.getString("objectId") ?: ""
            ObjectDetailScreen(navController, objectId = objectId)
        }
    }
}