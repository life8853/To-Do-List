package com.example.todolist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val categories = Category.entries
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1F6E3F))
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { innerPadding ->
        if (viewModel.currentSettings == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                SettingsSectionTitle(title = "Task Visibility")
                Card {
                    ListItem(
                        headlineContent = { Text("Hide completed tasks") },
                        trailingContent = {
                            Switch(
                                checked = viewModel.currentSettings!!.hideCompleted,
                                onCheckedChange = { viewModel.updateHideCompleted(it) }
                            )
                        }
                    )
                }

                SettingsSectionTitle(title = "Visible Categories")
                Card {
                    Column {
                        categories.forEach { category ->
                            ListItem(
                                headlineContent = { Text(category.displayName) },
                                trailingContent = {
                                    Checkbox(
                                        checked = viewModel.currentSettings!!.visibleCategories.contains(
                                            category
                                        ),
                                        onCheckedChange = { isChecked ->
                                            viewModel.updateCategory(category, isChecked)
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                SettingsSectionTitle(title = "Notifications")
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Notification time: ${viewModel.currentSettings!!.notificationTime} minutes before",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = viewModel.currentSettings!!.notificationTime.toFloat(),
                            onValueChange = { viewModel.updateNotificationTime(it.toInt()) },
                            valueRange = 0f..60f,
                            steps = 11
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = Color.Black
    )
}