package com.example.todolist

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.todolist.ui.theme.TextDark
import com.example.todolist.ui.theme.TextDarkGray
import kotlinx.coroutines.launch

@Composable
fun LocationPickerSection(
    latitude: String,
    longitude: String,
    radius: String,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onRadiusChange: (String) -> Unit,
    isLocationNotificationEnabled: Boolean
) {
    val context = LocalContext.current
    var mapsLinkInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoadingLocation by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Permission launcher for location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, fetch current location
            isLoadingLocation = true
            scope.launch {
                try {
                    val currentLocation = LocationUtils.getCurrentLocation(context)
                    if (currentLocation != null) {
                        onLatitudeChange(currentLocation.first.toString())
                        onLongitudeChange(currentLocation.second.toString())
                        showError = false
                        errorMessage = ""
                    } else {
                        showError = true
                        errorMessage = "Unable to get current location. Please ensure:\n" +
                                "• Location services are ON\n" +
                                "• GPS is enabled\n" +
                                "• You have a clear view of the sky\n" +
                                "• Try again in a few seconds"
                    }
                } finally {
                    isLoadingLocation = false
                }
            }
        } else {
            showError = true
            errorMessage = "Location permission denied. Please enable in Settings > App Details > Permissions"
        }
    }

    if (!isLocationNotificationEnabled) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Title
        Text(
            "Set Location for Notification",
            style = MaterialTheme.typography.labelMedium,
            color = TextDark
        )

        // Google Maps Link Input
        OutlinedTextField(
            value = mapsLinkInput,
            onValueChange = { mapsLinkInput = it },
            label = { Text("Paste Google Maps Link") },
            placeholder = { Text("or coordinates: 40.7128,-74.0060") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                if (mapsLinkInput.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            // Parse the link
                            val result = LocationUtils.parseGoogleMapsLink(mapsLinkInput)
                            if (result != null) {
                                onLatitudeChange(result.first.toString())
                                onLongitudeChange(result.second.toString())
                                showError = false
                                mapsLinkInput = ""
                            } else {
                                showError = true
                                errorMessage = "Invalid Google Maps link or coordinates format"
                            }
                        }
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = "Parse location",
                            tint = if (showError) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )

        // Current Location Button
        Button(
            onClick = {
                // Check permission first
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission already granted, fetch location
                    isLoadingLocation = true
                    scope.launch {
                        try {
                            val currentLocation = LocationUtils.getCurrentLocation(context)
                            if (currentLocation != null) {
                                onLatitudeChange(currentLocation.first.toString())
                                onLongitudeChange(currentLocation.second.toString())
                                showError = false
                                errorMessage = ""
                            } else {
                                showError = true
                                errorMessage = "Unable to get current location. Please ensure:\n" +
                                        "• Location services are ON\n" +
                                        "• GPS is enabled\n" +
                                        "• You have a clear view of the sky\n" +
                                        "• Try again in a few seconds"
                            }
                        } finally {
                            isLoadingLocation = false
                        }
                    }
                } else {
                    // Request permission
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            enabled = !isLoadingLocation,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoadingLocation) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Getting Location...")
            } else {
                Icon(Icons.Filled.LocationOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use Current Location")
            }
        }

        // Display current coordinates
        if (latitude.isNotEmpty() && longitude.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF0F8FF) // Light blue
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Selected Location:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDarkGray
                    )
                    Text(
                        "Lat: $latitude, Lng: $longitude",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDark
                    )

                    // Open in Maps button
                    val lat = latitude.toDoubleOrNull()
                    val lng = longitude.toDoubleOrNull()
                    if (lat != null && lng != null) {
                        TextButton(
                            onClick = {
                                val mapsUrl = LocationUtils.createGoogleMapsUrl(lat, lng)
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(mapsUrl)
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("Open in Google Maps")
                        }
                    }
                }
            }
        }

        // Radius Input
        OutlinedTextField(
            value = radius,
            onValueChange = onRadiusChange,
            label = { Text("Notification Radius (meters)") },
            placeholder = { Text("e.g., 100") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )


        // Error Message
        if (showError) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE) // Light red
                )
            ) {
                Text(
                    errorMessage,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFC62828) // Dark red
                )
            }
        }
    }
}

