package com.example.todolist

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) : GeofenceService {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    override fun addGeofence(requestId: Int, latitude: Double, longitude: Double, radiusMeters: Int) {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.w("GeofenceManager", "Location permission not granted, cannot add geofence")
            return
        }

        val geofence = Geofence.Builder()
            .setRequestId(requestId.toString())
            .setCircularRegion(latitude, longitude, radiusMeters.toFloat())
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val pendingIntent = getGeofencePendingIntent()

        geofencingClient.addGeofences(request, pendingIntent).addOnSuccessListener {
            Log.d("GeofenceManager", "Geofence added for requestId=$requestId")
        }.addOnFailureListener { e ->
            Log.e("GeofenceManager", "Failed to add geofence: ${e.message}")
        }
    }

    override fun removeGeofence(requestId: Int) {
        geofencingClient.removeGeofences(listOf(requestId.toString())).addOnSuccessListener {
            Log.d("GeofenceManager", "Geofence removed for requestId=$requestId")
        }.addOnFailureListener { e ->
            Log.e("GeofenceManager", "Failed to remove geofence: ${e.message}")
        }
    }

    private fun getGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        intent.action = "com.example.todolist.GEOFENCE_EVENT"
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

