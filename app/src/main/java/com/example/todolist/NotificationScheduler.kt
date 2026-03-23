package com.example.todolist

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.ZoneOffset

class NotificationScheduler(private val context: Context) {
    
    fun scheduleNotification(task: Task, minutesBeforeNotification: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val canExactBeScheduled = alarmManager.canScheduleExactAlarms()
        if (!canExactBeScheduled) {
            return
        }

        val dueDate = LocalDateTime.parse(task.dueTime)
        val alarmDateTime = dueDate.minusMinutes(minutesBeforeNotification.toLong())

        if (alarmDateTime.isBefore(LocalDateTime.now())) return

        val intent = Intent(context, TaskNotificationReceiver::class.java).apply {
            putExtra("task_title", task.title)
            putExtra("task_id", task.uid)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.uid,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTimestamp = alarmDateTime
            .atZone(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTimestamp,
            pendingIntent
        )
    }

    fun cancelNotification(taskId: Int) {
        val intent = Intent(context, TaskNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, taskId, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }
    }
}