package com.example.todolist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import android.app.Application
import com.example.todolist.ui.theme.PrimaryGreen
import com.example.todolist.ui.theme.LightBackground
import com.example.todolist.ui.theme.TextDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskList(
    onFabClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onStatsClick: () -> Unit,
    onTaskClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: TaskListViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TaskListViewModel(
                    application = context.applicationContext as Application
                ) as T
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("To-Do List") },
                actions = {
                    IconButton(onClick = onStatsClick) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Statistics"
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryGreen,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onFabClick
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Task")
            }
        }) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(color = LightBackground)
                .padding(innerPadding)
        ) {
            if (viewModel.tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks yet!",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextDark
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(
                        items = viewModel.tasks,
                        key = { it.task.uid }
                    ) { item ->
                        TaskItem(
                            task = item.task,
                            hasAttachments = item.attachments.isNotEmpty(),
                            onToggleCompleted = { viewModel.toggleTaskCompletion(item.task) },
                            onDelete = { viewModel.deleteTask(item) },
                            modifier = Modifier.clickable {
                                onTaskClick(item.task.uid)
                            }
                        )
                    }
                }
            }
        }
    }
}