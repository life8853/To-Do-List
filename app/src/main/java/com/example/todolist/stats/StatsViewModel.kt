package com.example.todolist.stats
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.TaskDatabase
import com.example.todolist.TaskWithAttachments
import com.example.todolist.Task
import com.example.todolist.TaskRepository
import com.example.todolist.NotificationScheduler
import com.example.todolist.NotificationService
import com.example.todolist.GeofenceManager
import com.example.todolist.GeofenceService
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.io.File

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    var overallStats by mutableStateOf<OverallStats?>(null)
        private set
    var allTasks by mutableStateOf<List<TaskWithAttachments>>(emptyList())
        private set
    var completedTasks by mutableStateOf<List<TaskWithAttachments>>(emptyList())
        private set
    var pendingTasks by mutableStateOf<List<TaskWithAttachments>>(emptyList())
        private set

    private val repository = TaskRepository(TaskDatabase.getDatabase(application).taskDao())
    private val notifier: NotificationService = NotificationScheduler(application)
    private val geofenceService: GeofenceService = GeofenceManager(application)

    private val statisticsCalculator: StatisticsCalculator = DefaultStatisticsCalculator()
    private val completedTasksFilter = CompletedTasksFilter()
    private val pendingTasksFilter = PendingTasksFilter()
    private val allTasksFilter = AllTasksFilter()

    init {
        viewModelScope.launch {
            repository.getTasksWithAttachmentsFlow().collect { tasks ->
                allTasks = allTasksFilter.filter(tasks)
                completedTasks = completedTasksFilter.filter(tasks)
                pendingTasks = pendingTasksFilter.filter(tasks)
                overallStats = statisticsCalculator.calculateOverallStats(tasks)
            }
        }
    }

    fun getTasksByCategory(categoryId: Int): List<TaskWithAttachments> {
        val categoryFilter = CategoryTasksFilter(categoryId)
        return categoryFilter.filter(allTasks)
    }

    fun getCategoryStats(categoryId: Int, categoryName: String): CategoryStats? {
        return overallStats?.categoryStats?.find { it.categoryId == categoryId }
            ?: run {
                val categoryTasks = getTasksByCategory(categoryId)
                statisticsCalculator.calculateCategoryStats(categoryTasks, categoryId, categoryName)
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
