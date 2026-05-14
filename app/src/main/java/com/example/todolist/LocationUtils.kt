package com.example.todolist

import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import kotlin.math.roundToInt

object LocationUtils {

    /**
     * Parse Google Maps URL and extract latitude and longitude
     * Supports formats like:
     * - https://maps.google.com/?q=40.7128,-74.0060
     * - https://maps.google.com/maps?q=40.7128,-74.0060
     * - https://www.google.com/maps/place/40.7128,-74.0060
     * - https://maps.app.goo.gl/sharedhash (returns null - requires API call)
     */
    fun parseGoogleMapsLink(url: String): Pair<Double, Double>? {
        return try {
            val cleanUrl = url.trim()

            // Try to extract coordinates from URL
            when {
                // Format: https://maps.google.com/?q=40.7128,-74.0060
                cleanUrl.contains("maps.google.com") && cleanUrl.contains("?q=") -> {
                    val query = cleanUrl.substringAfter("?q=").substringBefore("&")
                    val parts = query.split(",")
                    if (parts.size == 2) {
                        val lat = parts[0].toDoubleOrNull()
                        val lng = parts[1].toDoubleOrNull()
                        if (lat != null && lng != null) lat to lng else null
                    } else null
                }

                // Format: https://maps.google.com/maps?q=40.7128,-74.0060
                cleanUrl.contains("maps.google.com/maps") && cleanUrl.contains("q=") -> {
                    val query = cleanUrl.substringAfter("q=").substringBefore("&")
                    val parts = query.split(",")
                    if (parts.size == 2) {
                        val lat = parts[0].toDoubleOrNull()
                        val lng = parts[1].toDoubleOrNull()
                        if (lat != null && lng != null) lat to lng else null
                    } else null
                }

                // Format: https://www.google.com/maps/place/40.7128,-74.0060
                cleanUrl.contains("google.com/maps/place/") -> {
                    val coords = cleanUrl.substringAfter("place/").substringBefore("/")
                    val parts = coords.split(",")
                    if (parts.size == 2) {
                        val lat = parts[0].toDoubleOrNull()
                        val lng = parts[1].toDoubleOrNull()
                        if (lat != null && lng != null) lat to lng else null
                    } else null
                }

                // Format: lat,lng as plain text
                !cleanUrl.contains("http") -> {
                    val parts = cleanUrl.split(",")
                    if (parts.size == 2) {
                        val lat = parts[0].trim().toDoubleOrNull()
                        val lng = parts[1].trim().toDoubleOrNull()
                        if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                            lat to lng
                        } else null
                    } else null
                }

                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get user's current location (requires permission)
     * Returns null if location is not available
     */
    suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {
        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            // Check if we have location permission
            @Suppress("MissingPermission")
            val location = Tasks.await(fusedLocationClient.lastLocation)

            if (location != null) {
                Pair(location.latitude, location.longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Format location as a string for display
     */
    fun formatLocation(latitude: Double, longitude: Double): String {
        return String.format("%.4f, %.4f", latitude, longitude)
    }

    /**
     * Create a Google Maps URL from coordinates
     */
    fun createGoogleMapsUrl(latitude: Double, longitude: Double): String {
        return "https://maps.google.com/?q=$latitude,$longitude"
    }

    /**
     * Validate if coordinates are within valid ranges
     */
    fun isValidCoordinates(latitude: Double, longitude: Double): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    /**
     * Calculate distance in meters between two coordinates
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
}

