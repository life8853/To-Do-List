package com.example.todolist

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.app.AlarmManager
import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import android.content.pm.PackageManager
import com.example.todolist.stats.StatsScreen
import com.example.todolist.ui.theme.ToDoListTheme
import kotlinx.serialization.Serializable

const val DEEP_URI = "https://com.example.todolist"

@Serializable
object ListRoute

@Serializable
data class AddRoute(val taskId: Int? = null)

@Serializable
object SettingsRoute

@Serializable
object StatsRoute


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToDoListTheme {
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        android.util.Log.d("NotificationPermission", "Notification permission granted")
                    } else {
                        android.util.Log.d("NotificationPermission", "Notification permission denied")
                    }
                }

                val scheduleExactAlarmLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    android.util.Log.d("ScheduleAlarmPermission", "Returned from exact alarm settings")
                }

                LaunchedEffect(Unit) {
                    // Request notification permission on Android 13 (API 33) and above
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            android.util.Log.d("NotificationPermission", "Requesting POST_NOTIFICATIONS permission")
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    // Request exact alarm capability on Android 12 (API 31) and above
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                        if (!alarmManager.canScheduleExactAlarms()) {
                            android.util.Log.d("ScheduleAlarmPermission", "Requesting exact alarm permission via settings")
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            scheduleExactAlarmLauncher.launch(intent)
                        } else {
                            android.util.Log.d("ScheduleAlarmPermission", "Exact alarms already allowed")
                        }
                    }
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
                            onStatsClick = { navController.navigate(StatsRoute) },
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

                    composable<StatsRoute> {
                        StatsScreen(onBackClick = exitTaskEditor)
                    }
                }
            }
        }
    }
}