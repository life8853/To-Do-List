package com.example.todolist

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.example.todolist.ui.theme.ToDoListTheme
import kotlinx.serialization.Serializable

const val DEEP_URI = "https://com.example.todolist"

@Serializable
object ListRoute

@Serializable
data class AddRoute(val taskId: Int? = null)

@Serializable
object SettingsRoute


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToDoListTheme {
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {}

                LaunchedEffect(Unit) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = ListRoute,
                ) {
                    val exitTaskEditor = {
                        if (!navController.popBackStack()) {
                            navController.navigate(ListRoute) {
                                launchSingleTop = true
                            }
                        }
                    }

                    composable<ListRoute> {
                        TaskList(
                            onFabClick = { navController.navigate(AddRoute(taskId = null)) },
                            onSettingsClick = { navController.navigate(SettingsRoute) },
                            onTaskClick = { id -> navController.navigate(AddRoute(taskId = id)) }
                        )
                    }

                    composable<AddRoute>(
                        deepLinks = listOf(
                            navDeepLink<AddRoute>(basePath = DEEP_URI)
                        )
                    ) { backstackEntry ->
                        val route: AddRoute = backstackEntry.toRoute()
                        TaskAdder(taskID = route.taskId, onBack = exitTaskEditor)
                    }

                    composable<SettingsRoute> {
                        SettingsScreen()
                    }
                }
            }
        }
    }
}