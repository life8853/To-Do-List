package com.example.todolist.stats

import com.example.todolist.TaskWithAttachments

/**
 * Data class representing statistics for a single category
 */
data class CategoryStats(
    val categoryId: Int,
    val categoryName: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val pendingTasks: Int,
    val completionPercentage: Float
)

/**
 * Data class for overall statistics
 */
data class OverallStats(
    val totalTasks: Int,
    val completedTasks: Int,
    val pendingTasks: Int,
    val completionPercentage: Float,
    val categoryStats: List<CategoryStats>
)

/**
 * Interface for calculating statistics
 * Follows the Strategy pattern for extensibility
 */
interface StatisticsCalculator {
    fun calculateOverallStats(tasks: List<TaskWithAttachments>): OverallStats
    fun calculateCategoryStats(tasks: List<TaskWithAttachments>, categoryId: Int, categoryName: String): CategoryStats
}

/**
 * Concrete implementation of statistics calculator
 * Implements OOP principles: Single Responsibility, Encapsulation
 */
class DefaultStatisticsCalculator : StatisticsCalculator {

    override fun calculateOverallStats(tasks: List<TaskWithAttachments>): OverallStats {
        val totalTasks = tasks.size
        val completedTasks = tasks.count { it.task.completed }
        val pendingTasks = totalTasks - completedTasks
        val completionPercentage = if (totalTasks > 0) {
            (completedTasks.toFloat() / totalTasks) * 100
        } else {
            0f
        }

        val categoryStats = tasks
            .groupBy { it.task.category }
            .map { (categoryId, categoryTasks) ->
                val categoryName = getCategoryName(categoryId)
                calculateCategoryStats(categoryTasks, categoryId, categoryName)
            }

        return OverallStats(
            totalTasks = totalTasks,
            completedTasks = completedTasks,
            pendingTasks = pendingTasks,
            completionPercentage = completionPercentage,
            categoryStats = categoryStats
        )
    }

    override fun calculateCategoryStats(
        tasks: List<TaskWithAttachments>,
        categoryId: Int,
        categoryName: String
    ): CategoryStats {
        val totalTasks = tasks.size
        val completedTasks = tasks.count { it.task.completed }
        val pendingTasks = totalTasks - completedTasks
        val completionPercentage = if (totalTasks > 0) {
            (completedTasks.toFloat() / totalTasks) * 100
        } else {
            0f
        }

        return CategoryStats(
            categoryId = categoryId,
            categoryName = categoryName,
            totalTasks = totalTasks,
            completedTasks = completedTasks,
            pendingTasks = pendingTasks,
            completionPercentage = completionPercentage
        )
    }

    private fun getCategoryName(categoryId: Int): String {
        return when (categoryId) {
            0 -> "Health"
            1 -> "Education"
            2 -> "Exercise"
            3 -> "Work"
            4 -> "Shopping"
            else -> "Unknown"
        }
    }
}

/**
 * Filter interface for filtering tasks
 * Follows the Strategy pattern for different filtering approaches
 */
interface TaskFilter {
    fun filter(tasks: List<TaskWithAttachments>): List<TaskWithAttachments>
}

/**
 * Filters tasks showing only completed tasks
 */
class CompletedTasksFilter : TaskFilter {
    override fun filter(tasks: List<TaskWithAttachments>): List<TaskWithAttachments> {
        return tasks.filter { it.task.completed }
    }
}

/**
 * Filters tasks showing only pending tasks
 */
class PendingTasksFilter : TaskFilter {
    override fun filter(tasks: List<TaskWithAttachments>): List<TaskWithAttachments> {
        return tasks.filter { !it.task.completed }
    }
}

/**
 * Shows all tasks
 */
class AllTasksFilter : TaskFilter {
    override fun filter(tasks: List<TaskWithAttachments>): List<TaskWithAttachments> {
        return tasks
    }
}

/**
 * Filters tasks by category
 */
class CategoryTasksFilter(private val categoryId: Int) : TaskFilter {
    override fun filter(tasks: List<TaskWithAttachments>): List<TaskWithAttachments> {
        return tasks.filter { it.task.category == categoryId }
    }
}

