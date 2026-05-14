package com.example.todolist

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.LocalDateTime
import java.time.ZoneId

class NotificationScheduler(private val context: Context) : NotificationService {

    override fun scheduleTimeNotification(task: Task, minutesBeforeNotification: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check if device can schedule exact alarms (API 31+)
        val canExactBeScheduled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (!canExactBeScheduled) {
            Log.w("NotificationScheduler", "Cannot schedule exact alarms - device may not support it")
            return
        }

        val dueDate = LocalDateTime.parse(task.dueTime)
        val alarmDateTime = dueDate.minusMinutes(minutesBeforeNotification.toLong())

        if (alarmDateTime.isBefore(LocalDateTime.now())) {
            Log.d("NotificationScheduler", "Alarm time is in the past, skipping: $alarmDateTime")
            return
        }

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

        // Convert LocalDateTime to milliseconds using device timezone
        val triggerTimestamp = alarmDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        Log.d("NotificationScheduler", "Scheduling notification for task '${task.title}' at $alarmDateTime (${triggerTimestamp}ms from epoch)")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimestamp,
                pendingIntent
            )
        } else {
            // Fallback for older Android versions
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimestamp,
                pendingIntent
            )
        }
    }

    override fun cancelNotification(taskId: Int) {
        val intent = Intent(context, TaskNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, taskId, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            Log.d("NotificationScheduler", "Cancelled notification for task ID: $taskId")
        }
    }
}