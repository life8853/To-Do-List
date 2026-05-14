package com.example.todolist

interface GeofenceService {
	fun addGeofence(requestId: Int, latitude: Double, longitude: Double, radiusMeters: Int)
	fun removeGeofence(requestId: Int)
}

