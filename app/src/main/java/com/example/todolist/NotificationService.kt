package com.example.todolist

import android.content.Context

interface NotificationService {
    fun scheduleTimeNotification(task: Task, minutesBeforeNotification: Int)
    fun cancelNotification(taskId: Int)
}

