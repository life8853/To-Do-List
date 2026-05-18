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
        Log.d("NotificationScheduler", "scheduleTimeNotification called for task: ${task.title} (ID: ${task.uid})")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check if device can schedule exact alarms (API 31+)
        val canExactBeScheduled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms().also { canSchedule ->
                Log.d("NotificationScheduler", "canScheduleExactAlarms: $canSchedule")
            }
        } else {
            true
        }

        if (!canExactBeScheduled) {
            Log.w("NotificationScheduler", "Cannot schedule exact alarms - falling back to inexact alarms")
        }

        val dueDate = LocalDateTime.parse(task.dueTime)
        val now = LocalDateTime.now()
        val beforeDateTime = dueDate.minusMinutes(minutesBeforeNotification.toLong())

        if (beforeDateTime.isAfter(now) && minutesBeforeNotification > 0) {
            scheduleSingleNotification(
                alarmManager = alarmManager,
                canExactBeScheduled = canExactBeScheduled,
                task = task,
                triggerDateTime = beforeDateTime,
                requestCode = getBeforeRequestCode(task.uid),
                notificationId = getBeforeNotificationId(task.uid),
                notificationType = "before",
                minutesBefore = minutesBeforeNotification
            )
        } else {
            Log.d("NotificationScheduler", "Skipping before-notification; time is in the past or minutesBeforeNotification <= 0")
        }

        if (dueDate.isAfter(now)) {
            scheduleSingleNotification(
                alarmManager = alarmManager,
                canExactBeScheduled = canExactBeScheduled,
                task = task,
                triggerDateTime = dueDate,
                requestCode = getAtTimeRequestCode(task.uid),
                notificationId = getAtTimeNotificationId(task.uid),
                notificationType = "at_time",
                minutesBefore = 0
            )
        } else {
            Log.d("NotificationScheduler", "Skipping at-time notification; due time is in the past")
        }
    }

    override fun cancelNotification(taskId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        cancelSingleNotification(alarmManager, taskId, getBeforeRequestCode(taskId))
        cancelSingleNotification(alarmManager, taskId, getAtTimeRequestCode(taskId))
        Log.d("NotificationScheduler", "Cancelled notifications for task ID: $taskId")
    }

    private fun scheduleSingleNotification(
        alarmManager: AlarmManager,
        canExactBeScheduled: Boolean,
        task: Task,
        triggerDateTime: LocalDateTime,
        requestCode: Int,
        notificationId: Int,
        notificationType: String,
        minutesBefore: Int
    ) {
        val intent = Intent(context, TaskNotificationReceiver::class.java).apply {
            putExtra("task_title", task.title)
            putExtra("task_id", task.uid)
            putExtra("notification_id", notificationId)
            putExtra("notification_type", notificationType)
            putExtra("minutes_before", minutesBefore)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTimestamp = triggerDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val currentTime = System.currentTimeMillis()
        Log.d(
            "NotificationScheduler",
            "Scheduling ${notificationType} notification for task '${task.title}' at $triggerDateTime (${triggerTimestamp}ms from epoch, in ${(triggerTimestamp - currentTime) / 1000} seconds)"
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (canExactBeScheduled) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimestamp,
                        pendingIntent
                    )
                    Log.d("NotificationScheduler", "Scheduled exact alarm (RTC_WAKEUP)")
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTimestamp,
                        pendingIntent
                    )
                    Log.d("NotificationScheduler", "Scheduled inexact alarm with \"allow while idle\" (exact not available)")
                }
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimestamp,
                    pendingIntent
                )
                Log.d("NotificationScheduler", "Scheduled alarm with \"allow while idle\" (older Android version)")
            }
        } catch (e: SecurityException) {
            Log.e("NotificationScheduler", "SecurityException: Unable to schedule alarm - check SCHEDULE_EXACT_ALARM permission", e)
        } catch (e: Exception) {
            Log.e("NotificationScheduler", "Error scheduling alarm: ${e.message}", e)
        }
    }

    private fun cancelSingleNotification(
        alarmManager: AlarmManager,
        taskId: Int,
        requestCode: Int
    ) {
        val intent = Intent(context, TaskNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            Log.d("NotificationScheduler", "Cancelled alarm for task ID: $taskId (requestCode: $requestCode)")
        }
    }

    private fun getBeforeRequestCode(taskId: Int): Int = taskId * 2

    private fun getAtTimeRequestCode(taskId: Int): Int = taskId * 2 + 1

    private fun getBeforeNotificationId(taskId: Int): Int = taskId * 10 + 1

    private fun getAtTimeNotificationId(taskId: Int): Int = taskId * 10 + 2
}