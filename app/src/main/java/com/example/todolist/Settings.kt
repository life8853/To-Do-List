package com.example.todolist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todolist.ui.theme.PrimaryGreen
import com.example.todolist.ui.theme.LightBackground
import com.example.todolist.ui.theme.TextDark
import com.example.todolist.ui.theme.TextDarkGray
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val categories = Category.entries
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryGreen)
            )
        },
        containerColor = LightBackground
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
                        val notificationOptions = remember { listOf(2, 4, 8, 16, 32, 64) }
                        var notificationExpanded by remember { mutableStateOf(false) }

                        val currentValue = viewModel.currentSettings!!.notificationTime
                        val normalizedValue = notificationOptions.minBy { abs(it - currentValue) }

                        LaunchedEffect(currentValue) {
                            if (!notificationOptions.contains(currentValue)) {
                                viewModel.updateNotificationTime(normalizedValue)
                            }
                        }

                        Text(
                            text = "Notification time",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ExposedDropdownMenuBox(
                            expanded = notificationExpanded,
                            onExpandedChange = { notificationExpanded = !notificationExpanded }
                        ) {
                            OutlinedTextField(
                                value = "${viewModel.currentSettings!!.notificationTime} minutes before",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Notify before") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = notificationExpanded
                                    )
                                },
                                modifier = Modifier
                                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            DropdownMenu(
                                expanded = notificationExpanded,
                                onDismissRequest = { notificationExpanded = false }
                            ) {
                                notificationOptions.forEach { minutes ->
                                    DropdownMenuItem(
                                        text = { Text("$minutes minutes before") },
                                        onClick = {
                                            viewModel.updateNotificationTime(minutes)
                                            notificationExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Options: 2, 4, 8, 16, 32, or 64 minutes",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDarkGray,
                            modifier = Modifier.padding(top = 8.dp)
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
        color = TextDark
    )
}
