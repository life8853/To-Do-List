package com.example.todolist

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationUtils {

    private const val TAG = "locationutils"

    /**
     * Parse Google Maps URL and extract latitude and longitude
     * Supports formats like:
     * - https://maps.google.com/?q=40.7128,-74.0060
     * - https://maps.google.com/maps?q=40.7128,-74.0060
     * - https://www.google.com/maps/place/40.7128,-74.0060
     * - https://www.google.com/maps/place/[name]/@40.7128,-74.0060,z...
     * - https://www.google.com/maps/search/[query]/@40.7128,-74.0060,z...
     * - https://maps.app.goo.gl/sharedhash (shortened link - needs expansion)
     * - lat,lng as plain text (e.g., 40.7128,-74.0060)
     */
    fun parseGoogleMapsLink(url: String): Pair<Double, Double>? {
        return try {
            val cleanUrl = url.trim()

            // Check if it's a shortened link
            if (cleanUrl.contains("maps.app.goo.gl")) {
                Log.w(TAG, "Shortened Google Maps link detected - cannot parse without expansion")
                return null // Return null to trigger error message about shortened links
            }

            // Try to extract coordinates from URL
            when {
                // Format: https://www.google.com/maps/place/[name]/@40.7128,-74.0060,z...
                // or: https://www.google.com/maps/search/[query]/@40.7128,-74.0060,z...
                cleanUrl.contains("google.com/maps/") && cleanUrl.contains("/@") -> {
                    val coordsPart = cleanUrl.substringAfter("/@").substringBefore(",z")
                    if (coordsPart.isEmpty()) {
                        // Try without z suffix
                        val altCoordsPart = cleanUrl.substringAfter("/@")
                        val parts = altCoordsPart.split(",")
                        if (parts.size >= 2) {
                            val lat = parts[0].toDoubleOrNull()
                            val lng = parts[1].toDoubleOrNull()
                            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                                lat to lng
                            } else null
                        } else null
                    } else {
                        val parts = coordsPart.split(",")
                        if (parts.size >= 2) {
                            val lat = parts[0].toDoubleOrNull()
                            val lng = parts[1].toDoubleOrNull()
                            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                                lat to lng
                            } else null
                        } else null
                    }
                }

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
            Log.e(TAG, "Error parsing Google Maps link: ${e.message}")
            null
        }
    }

    /**
     * Get user's current location (requires permission)
     * First tries last known location, then requests fresh location if unavailable
     * Returns null if location is not available after timeout
     */
    suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {
        return try {
            Log.d(TAG, "=== getCurrentLocation START ===")

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            Log.d(TAG, "LocationManager obtained")

            // Check if location services are enabled (API 28+)
            val isLocationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                locationManager.isLocationEnabled.also { enabled ->
                    Log.d(TAG, "API 28+: isLocationEnabled = $enabled")
                }
            } else {
                @Suppress("DEPRECATION")
                (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).also { enabled ->
                    Log.d(TAG, "API <28: GPS or Network enabled = $enabled")
                }
            }

            if (!isLocationEnabled) {
                Log.w(TAG, "Location services are disabled on device")
                return null
            }

            Log.d(TAG, "Location services are ENABLED")
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            Log.d(TAG, "FusedLocationProviderClient obtained")

            // First, try to get last known location (fastest but may be stale)
            Log.d(TAG, "Attempting to get last known location...")
            val lastLocation = getLastKnownLocation(fusedLocationClient)

            if (lastLocation != null) {
                Log.d(TAG, "✅ Got last known location: ${lastLocation.latitude}, ${lastLocation.longitude}")
                return Pair(lastLocation.latitude, lastLocation.longitude)
            }

            Log.d(TAG, "Last known location is null, trying getCurrentLocation...")
            val singleUpdateLocation = getCurrentLocationOnce(fusedLocationClient)
            if (singleUpdateLocation != null) {
                Log.d(TAG, "✅ Got getCurrentLocation: ${singleUpdateLocation.latitude}, ${singleUpdateLocation.longitude}")
                return Pair(singleUpdateLocation.latitude, singleUpdateLocation.longitude)
            }

            Log.d(TAG, "getCurrentLocation returned null, requesting fresh location update...")

            // If still no location, request a fresh location update
            val freshLocation = requestFreshLocation(fusedLocationClient)

            Log.d(TAG, if (freshLocation != null) "✅ Got fresh location: ${freshLocation.first}, ${freshLocation.second}" else "❌ Fresh location request timed out or failed")

            return freshLocation

        } catch (e: Exception) {
            Log.e(TAG, "EXCEPTION in getCurrentLocation: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }

    /**
     * Request a fresh location update with timeout
     * This is used when last known location is not available
     */
    private suspend fun requestFreshLocation(
        fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
    ): Pair<Double, Double>? {
        return suspendCancellableCoroutine { continuation ->
            var locationCallback: LocationCallback? = null
            var timeoutRunnable: Runnable? = null
            val handler = Handler(Looper.getMainLooper())
            val timeoutMillis = 15000L // 15 second timeout

            Log.d(TAG, "requestFreshLocation: Starting fresh location request (${timeoutMillis}ms timeout)")

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    Log.d(TAG, "onLocationResult called")

                    val location = result.lastLocation
                    if (location != null) {
                        Log.d(TAG, "✅ Fresh location received: ${location.latitude}, ${location.longitude}")

                        // Remove timeout callback
                        if (timeoutRunnable != null) {
                            handler.removeCallbacks(timeoutRunnable!!)
                            Log.d(TAG, "Removed timeout callback")
                        }

                        // Remove location updates
                        try {
                            fusedLocationClient.removeLocationUpdates(this)
                            Log.d(TAG, "Removed location updates callback")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error removing location updates: ${e.message}")
                        }

                        // Resume with location
                        if (continuation.isActive) {
                            continuation.resume(Pair(location.latitude, location.longitude))
                        }
                    } else {
                        Log.w(TAG, "Location received but lastLocation is null")
                    }
                }
            }

            timeoutRunnable = Runnable {
                Log.w(TAG, "⏱️ Location request TIMEOUT - no location received in ${timeoutMillis}ms")

                // Remove location updates
                try {
                    @Suppress("MissingPermission")
                    fusedLocationClient.removeLocationUpdates(locationCallback!!)
                    Log.d(TAG, "Removed location updates after timeout")
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing location updates after timeout: ${e.message}")
                }

                // Resume with null
                if (continuation.isActive) {
                    continuation.resume(null)
                    Log.d(TAG, "Resumed continuation with null (timeout)")
                }
            }

            try {
                Log.d(TAG, "Requesting location updates with high accuracy...")

                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    5000 // Update interval: 5 seconds
                ).setMinUpdateDistanceMeters(0f)
                    .setMaxUpdateDelayMillis(10000) // Max wait: 10 seconds
                    .setWaitForAccurateLocation(true) // Wait for accurate location
                    .build()

                Log.d(TAG, "LocationRequest created: interval=5000ms, maxWait=10000ms, waitForAccurate=true")

                @Suppress("MissingPermission")
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback!!,
                    Looper.getMainLooper()
                )

                Log.d(TAG, "requestLocationUpdates() called, setting timeout...")

                // Set timeout
                handler.postDelayed(timeoutRunnable!!, timeoutMillis)
                Log.d(TAG, "Timeout scheduled for ${timeoutMillis}ms")

                // Handle cancellation
                continuation.invokeOnCancellation {
                    Log.d(TAG, "Continuation cancelled, cleaning up...")

                    if (timeoutRunnable != null) {
                        handler.removeCallbacks(timeoutRunnable!!)
                        Log.d(TAG, "Removed timeout callback")
                    }

                    try {
                        fusedLocationClient.removeLocationUpdates(locationCallback!!)
                        Log.d(TAG, "Removed location updates on cancellation")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error removing location updates on cancellation: ${e.message}")
                    }
                }

            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException: Missing location permission", e)

                if (timeoutRunnable != null) {
                    handler.removeCallbacks(timeoutRunnable!!)
                }

                if (continuation.isActive) {
                    continuation.resume(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception requesting location updates: ${e.message}", e)
                e.printStackTrace()

                if (timeoutRunnable != null) {
                    handler.removeCallbacks(timeoutRunnable!!)
                }

                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }

    @Suppress("MissingPermission")
    private suspend fun getLastKnownLocation(
        fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
    ): Location? {
        return suspendCancellableCoroutine { continuation ->
            val task: Task<Location> = fusedLocationClient.lastLocation
            task.addOnSuccessListener { location ->
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }.addOnFailureListener { e ->
                Log.w(TAG, "getLastKnownLocation failed: ${e.message}")
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }

            continuation.invokeOnCancellation {
                // No cancellation available for lastLocation, just ignore.
            }
        }
    }

    @Suppress("MissingPermission")
    private suspend fun getCurrentLocationOnce(
        fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
    ): Location? {
        return suspendCancellableCoroutine { continuation ->
            val tokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                tokenSource.token
            ).addOnSuccessListener { location ->
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }.addOnFailureListener { e ->
                Log.w(TAG, "getCurrentLocation failed: ${e.message}")
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }

            continuation.invokeOnCancellation {
                tokenSource.cancel()
            }
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

