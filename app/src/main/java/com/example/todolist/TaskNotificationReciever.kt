package com.example.todolist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import android.Manifest
import android.content.pm.PackageManager

class TaskNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("TaskNotificationReceiver", "Notification broadcast received")

        val title = intent.getStringExtra("task_title")
        val taskId = intent.getIntExtra("task_id", 0)
        val notificationId = intent.getIntExtra("notification_id", taskId)
        val notificationType = intent.getStringExtra("notification_type") ?: "before"
        val minutesBefore = intent.getIntExtra("minutes_before", 0)

        Log.d(
            "TaskNotificationReceiver",
            "Task Title: $title, Task ID: $taskId, Type: $notificationType, Notification ID: $notificationId"
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "task_reminders"

        val channel =
            NotificationChannel(channelId, "Task Reminders", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        val URI = "https://com.example.todolist?taskId=$taskId".toUri()
        val activityIntent = Intent(
            Intent.ACTION_VIEW,
            URI,
            context,
            MainActivity::class.java
        )

        Log.d("TaskNotificationReceiver", "Created URI: $URI")

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (notificationType == "at_time") {
            "Task is due now"
        } else if (minutesBefore > 0) {
            "Task is due in $minutesBefore minutes"
        } else {
            title
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Task Reminder")
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        Log.d("TaskNotificationReceiver", "Showing notification with ID: $notificationId")

        // Check POST_NOTIFICATIONS permission before showing notification (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("TaskNotificationReceiver", "POST_NOTIFICATIONS permission not granted, cannot show notification")
                return
            }
        }

        notificationManager.notify(notificationId, notification)
    }
}