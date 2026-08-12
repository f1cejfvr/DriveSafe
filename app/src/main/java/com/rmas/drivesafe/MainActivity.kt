package com.rmas.drivesafe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rmas.drivesafe.navigation.AppNavigation
import com.rmas.drivesafe.navigation.Screen
import com.rmas.drivesafe.ui.theme.DriveSafeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DriveSafeTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute !in listOf(
        Screen.Login.route,
        Screen.Register.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        AppNavigation(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavController, currentRoute: String?) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Screen.Map.route,
            onClick = {
                navController.navigate(Screen.Map.route) {
                    popUpTo(Screen.Map.route) { inclusive = true }
                }
            },
            icon = { Text("Mapa") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.ObjectList.route,
            onClick = {
                navController.navigate(Screen.ObjectList.route) {
                    popUpTo(Screen.Map.route)
                }
            },
            icon = { Text("Lista") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Leaderboard.route,
            onClick = {
                navController.navigate(Screen.Leaderboard.route) {
                    popUpTo(Screen.Map.route)
                }
            },
            icon = { Text("Rang") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Profile.route,
            onClick = {
                navController.navigate(Screen.Profile.route) {
                    popUpTo(Screen.Map.route)
                }
            },
            icon = { Text("Profil") }
        )
    }
}