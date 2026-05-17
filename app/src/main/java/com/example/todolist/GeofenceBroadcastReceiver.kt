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
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("GeofenceReceiver", "Geofence broadcast received")

        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: run {
            Log.w("GeofenceReceiver", "GeofencingEvent is null")
            return
        }

        if (geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "GeofencingEvent error: ${geofencingEvent.errorCode}")
            return
        }

        val transition = geofencingEvent.geofenceTransition
        Log.d("GeofenceReceiver", "Geofence transition: $transition")

        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggering = geofencingEvent.triggeringGeofences ?: run {
                Log.w("GeofenceReceiver", "No triggering geofences")
                return
            }

            triggering.forEach { geofence ->
                val requestId = geofence.requestId
                Log.d("GeofenceReceiver", "Triggered geofence: $requestId")

                val taskId = requestId.toIntOrNull() ?: return@forEach

                // fetch task title from DB asynchronously and show notification
                CoroutineScope(Dispatchers.IO).launch {
                    val task = TaskDatabase.getDatabase(context).taskDao().getTaskById(taskId)
                    val title = task?.title ?: "Task nearby"
                    Log.d("GeofenceReceiver", "Showing notification for task: $title (ID: $taskId)")

                    showNotification(context, taskId, title)
                }
            }
        }
    }

    private fun showNotification(context: Context, taskId: Int, title: String) {
        // Check POST_NOTIFICATIONS permission before showing notification (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("GeofenceReceiver", "POST_NOTIFICATIONS permission not granted, cannot show notification")
                return
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "task_reminders"
        val channel = NotificationChannel(channelId, "Task Reminders", NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)

        val URI = "https://com.example.todolist?taskId=$taskId".toUri()
        val activityIntent = Intent(Intent.ACTION_VIEW, URI, context, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Location Reminder")
            .setContentText(title)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        Log.d("GeofenceReceiver", "Showing location notification for taskId=$taskId")
        notificationManager.notify(taskId + 100000, notification)
    }
}

