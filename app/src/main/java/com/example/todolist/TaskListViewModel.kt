package com.example.todolist

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File

class TaskListViewModel(
    application: Application,
    private val repository: ITaskRepository = TaskRepository(
        TaskDatabase.getDatabase(application).taskDao()
    ),
    private val notifier: NotificationService = NotificationScheduler(application),
    private val geofenceService: GeofenceService = GeofenceManager(application)
) : AndroidViewModel(application) {

    var tasks by mutableStateOf<List<TaskWithAttachments>>(emptyList())
        private set

    private val settingsManager = SettingsManager.getInstance(application)

    init {
        viewModelScope.launch {
            combine(
                repository.getTasksWithAttachmentsFlow(),
                settingsManager.settingsFlow
            ) { tasks, settings ->
                tasks.filter { task ->
                    val isCategoryVisible =
                        settings.visibleCategories.any { it.id == task.task.category }
                    val isNotHiddenByStatus = !settings.hideCompleted || !task.task.completed

                    isCategoryVisible && isNotHiddenByStatus
                }
            }.collect { filteredList ->
                tasks = filteredList
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy()
            updatedTask.completed = !updatedTask.completed
            repository.updateTask(updatedTask)
        }
    }

    fun deleteTask(task: TaskWithAttachments) {
        viewModelScope.launch(Dispatchers.IO) {
            task.attachments.forEach { attachment ->
                try {
                    val file = File(attachment.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            repository.deleteTask(task.task)
            notifier.cancelNotification(task.task.uid)
            geofenceService.removeGeofence(task.task.uid)
        }
    }

}