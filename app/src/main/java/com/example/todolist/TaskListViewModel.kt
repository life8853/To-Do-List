package com.example.todolist

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File

class TaskListViewModel(application: Application) : AndroidViewModel(application) {

    var tasks by mutableStateOf<List<TaskWithAttachments>>(emptyList())
        private set

    var searchQuery by mutableStateOf("")
    private val taskDao = TaskDatabase.getDatabase(application).taskDao()
    private val settingsManager = SettingsManager.getInstance(application)

    private val _searchQuery = MutableStateFlow("")
    init {
        viewModelScope.launch {
            combine(
                taskDao.getTasksWithAttachments(),
                settingsManager.settingsFlow,
                _searchQuery
            ) { allTasks, settings, query ->

                allTasks
                    .filter { item ->
                        val isCategoryVisible =
                            settings.visibleCategories.any { it.id == item.task.category }
                        val isNotHiddenByStatus =
                            !settings.hideCompleted || !item.task.completed
                        isCategoryVisible && isNotHiddenByStatus
                    }
                    .filter { item ->
                        if (query.isBlank()) true
                        else item.task.title.contains(query, ignoreCase = true) ||
                                item.task.description.contains(query, ignoreCase = true)
                    }
                    .sortedWith(
                        compareByDescending<TaskWithAttachments> { it.task.priority }
                            .thenBy { it.task.dueTime }
                    )
            }.collect { filteredList ->
                tasks = filteredList
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery = query
        _searchQuery.value = query
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(completed = !task.completed)
            taskDao.update(updatedTask)
        }
    }

    fun deleteTask(task: TaskWithAttachments) {
        val notifier = NotificationScheduler(application)
        viewModelScope.launch(Dispatchers.IO) {
            task.attachments.forEach { attachment ->
                try {
                    val file = File(attachment.filePath)
                    if (file.exists()) file.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            taskDao.delete(task.task)
            notifier.cancelNotification(task.task.uid)
        }
    }
}