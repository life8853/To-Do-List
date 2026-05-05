package com.example.todolist.stats
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.todolist.TaskDatabase
import com.example.todolist.TaskWithAttachments
import kotlinx.coroutines.launch
class StatsViewModel(application: Application) : AndroidViewModel(application) {
    var overallStats by mutableStateOf<OverallStats?>(null)
        private set
    var allTasks by mutableStateOf<List<TaskWithAttachments>>(emptyList())
        private set
    var completedTasks by mutableStateOf<List<TaskWithAttachments>>(emptyList())
        private set
    var pendingTasks by mutableStateOf<List<TaskWithAttachments>>(emptyList())
        private set
    private val taskDao = TaskDatabase.getDatabase(application).taskDao()
    private val statisticsCalculator: StatisticsCalculator = DefaultStatisticsCalculator()
    private val completedTasksFilter = CompletedTasksFilter()
    private val pendingTasksFilter = PendingTasksFilter()
    private val allTasksFilter = AllTasksFilter()
    init {
        viewModelScope.launch {
            taskDao.getTasksWithAttachments().collect { tasks ->
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
}
