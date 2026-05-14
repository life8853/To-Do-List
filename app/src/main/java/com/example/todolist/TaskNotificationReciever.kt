package com.example.todolist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri

class TaskNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("TaskNotificationReceiver", "Notification broadcast received")

        val title = intent.getStringExtra("task_title")
        val taskId = intent.getIntExtra("task_id", 0)

        Log.d("TaskNotificationReceiver", "Task Title: $title, Task ID: $taskId")

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
            taskId,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Task Reminder")
            .setContentText(title)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        Log.d("TaskNotificationReceiver", "Showing notification with ID: $taskId")
        notificationManager.notify(taskId, notification)
    }
}