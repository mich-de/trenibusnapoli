package com.eavorari

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.eavorari.ui.screens.ScheduleScreen
import com.eavorari.ui.screens.SettingsScreen
import com.eavorari.ui.screens.StationSelectScreen
import com.eavorari.ui.theme.EavTheme
import com.eavorari.viewmodel.ScheduleViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EavTheme {
                EavOrariApp()
            }
        }
    }
}

@Composable
fun EavOrariApp() {
    val navController = rememberNavController()
    val viewModel: ScheduleViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "station_select",
    ) {
        composable("station_select") {
            StationSelectScreen(
                viewModel = viewModel,
                onNavigate = { station, tipo ->
                    viewModel.selectStation(station)
                    val encodedName = Uri.encode(station.name)
                    navController.navigate("schedule/${station.id}/$tipo/$encodedName")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
            )
        }

        composable(
            route = "schedule/{stationId}/{tipo}/{stationName}",
            arguments = listOf(
                navArgument("stationId") { type = NavType.StringType },
                navArgument("tipo") { type = NavType.StringType },
                navArgument("stationName") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val stationName = backStackEntry.arguments?.getString("stationName") ?: ""
            val tipo = backStackEntry.arguments?.getString("tipo") ?: "P"

            ScheduleScreen(
                viewModel = viewModel,
                stationName = stationName,
                initialTipo = tipo,
                onBack = { navController.popBackStack() },
            )
        }

        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onStationClicked = { station, tipo ->
                    navController.popBackStack()
                    viewModel.selectStation(station)
                    val encodedName = Uri.encode(station.name)
                    navController.navigate("schedule/${station.id}/$tipo/$encodedName")
                },
            )
        }
    }
}
