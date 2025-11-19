package com.veigar.questtracker.services

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.veigar.questtracker.data.GeofenceRepository
import com.veigar.questtracker.data.NotificationsRepository
import com.veigar.questtracker.model.ChildLocationData
import com.veigar.questtracker.model.GeofenceData
import com.veigar.questtracker.model.NotificationCategory
import com.veigar.questtracker.model.NotificationData
import com.veigar.questtracker.model.NotificationModel
import com.veigar.questtracker.model.UserModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LocationMonitor(
    private val context: Context,
    private val user: UserModel,
    private val intervalMillis: Long = 60000L, // 1 minute
    private val serviceScope: CoroutineScope
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            updateLocationIfConnected()
            handler.postDelayed(this, intervalMillis)
        }
    }

    private var isTracking = false
    private val geofences = mutableListOf<GeofenceData>()
    private var geofencesJob: Job? = null
    private var lastOutsideZoneNotificationTime: Long = 0
    private var lastLocationServicesNotificationTime: Long = 0
    private val notificationThrottleInterval = 300000L // 5 minutes

    fun startTracking() {
        if(!isTracking){
            isTracking = true
            // Load geofences
            loadGeofences()
            // Force immediate location update when starting
            updateLocationIfConnected()
            handler.post(runnable)
        }
    }

    fun stopTracking() {
        handler.removeCallbacks(runnable)
        geofencesJob?.cancel()
        geofencesJob = null
        isTracking = false
    }

    fun forceLocationUpdate() {
        updateLocationIfConnected()
    }

    /**
     * Load and observe geofence updates
     */
    private fun loadGeofences() {
        geofencesJob?.cancel()
        geofencesJob = serviceScope.launch(Dispatchers.IO) {
            if (user.parentLinkedId.isNullOrBlank()) {
                Log.w("LocationMonitor", "Parent not linked, cannot load geofences")
                return@launch
            }
            GeofenceRepository.getGeofenceUpdates(user.parentLinkedId!!).collectLatest { result ->
                result.fold(
                    onSuccess = { loadedGeofences ->
                        geofences.clear()
                        geofences.addAll(loadedGeofences)
                        Log.d("LocationMonitor", "Loaded ${loadedGeofences.size} geofences")
                    },
                    onFailure = { error ->
                        Log.e("LocationMonitor", "Failed to load geofences: ${error.message}")
                    }
                )
            }
        }
    }

    /**
     * Calculate distance between two points in meters
     */
    private fun calculateDistanceInMeters(point1: LatLng, point2: LatLng): Float {
        val location1 = Location("").apply {
            latitude = point1.latitude
            longitude = point1.longitude
        }
        val location2 = Location("").apply {
            latitude = point2.latitude
            longitude = point2.longitude
        }
        return location1.distanceTo(location2) // Returns distance in meters
    }

    /**
     * Check if location is outside all geofence zones
     */
    private fun isLocationOutsideAllGeofences(latitude: Double, longitude: Double, geofences: List<GeofenceData>): Boolean {
        if (geofences.isEmpty()) {
            return false // If no geofences, consider child as "inside" (no notification needed)
        }

        val location = LatLng(latitude, longitude)

        for (geofence in geofences) {
            val geofenceLocation = geofence.position
            val distance = calculateDistanceInMeters(location, geofenceLocation)

            // If child is within any geofence radius, they're not outside all zones
            if (distance <= geofence.radius) {
                return false
            }
        }

        // Child is outside all geofences
        return true
    }

    /**
     * Check if location services (GPS or Network) are enabled
     */
    private fun checkLocationServicesEnabled(): Boolean {
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return isGpsEnabled || isNetworkEnabled
    }

    /**
     * Notify child that location services need to be enabled
     */
    private fun notifyChildLocationServicesDisabled() {
        val now = System.currentTimeMillis()
        // Throttle notifications to avoid spamming
        if (now - lastLocationServicesNotificationTime < notificationThrottleInterval) {
            return
        }
        lastLocationServicesNotificationTime = now

        serviceScope.launch(Dispatchers.IO) {
            try {
                NotificationsRepository.sendNotification(
                    targetId = user.getDecodedUid(),
                    notification = NotificationModel(
                        title = "Location Services Required",
                        message = "Please enable location services (GPS or Network) to allow location tracking.",
                        timestamp = now,
                        category = NotificationCategory.SYSTEM,
                        notificationData = NotificationData(
                            content = "Location tracking is disabled. Enable location services in your device settings.",
                            action = "child"
                        )
                    )
                )
                Log.d("LocationMonitor", "Sent location services disabled notification to child")
            } catch (e: Exception) {
                Log.e("LocationMonitor", "Failed to send location services notification: ${e.message}")
            }
        }
    }

    /**
     * Notify parent that child is outside all geofence zones
     */
    private fun notifyParentChildOutsideZone(latitude: Double, longitude: Double) {
        val now = System.currentTimeMillis()
        // Throttle notifications to avoid spamming (only send if last notification was > 5 minutes ago)
        if (now - lastOutsideZoneNotificationTime < notificationThrottleInterval) {
            return
        }
        lastOutsideZoneNotificationTime = now

        serviceScope.launch(Dispatchers.IO) {
            try {
                if (user.parentLinkedId.isNullOrBlank()) {
                    Log.w("LocationMonitor", "Parent ID is null, cannot send outside zone notification")
                    return@launch
                }

                NotificationsRepository.sendNotification(
                    targetId = user.parentLinkedId!!,
                    notification = NotificationModel(
                        title = "${user.name} is Outside Zone",
                        message = "${user.name} is currently outside all designated safe zones. Location: ${String.format("%.4f", latitude)}, ${String.format("%.4f", longitude)}",
                        timestamp = now,
                        category = NotificationCategory.LOCATION_UPDATE,
                        notificationData = NotificationData(
                            content = "Child is outside all geofence zones",
                            action = "parent"
                        )
                    )
                )
                Log.d("LocationMonitor", "Sent outside zone notification to parent for ${user.name}")
            } catch (e: Exception) {
                Log.e("LocationMonitor", "Failed to send outside zone notification: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationIfConnected() {
        Log.d("LocationMonitor", "Updating location for ${user.getDecodedUid()}")

        // Check if location services are enabled before attempting to get location
        if (!checkLocationServicesEnabled()) {
            Log.w("LocationMonitor", "Location services are disabled")
            notifyChildLocationServicesDisabled()
            return
        }

        // FIX: Use HIGH_ACCURACY for realtime tracking and accurate updates
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location: Location? ->
            location?.let {
                // Less aggressive filtering for accuracy if we want "best available"
                if (location.accuracy > 200) {
                    Log.w("LocationMonitor", "Discarding inaccurate location with accuracy: ${location.accuracy}")
                    return@addOnSuccessListener
                }
                serviceScope.launch(Dispatchers.IO) {
                    Log.d("LocationMonitor", "Location: ${location.latitude}, ${location.longitude}")
                    val now = System.currentTimeMillis()

                    // Check if location is outside all geofences
                    val currentGeofences = geofences.toList() // Create a snapshot to avoid concurrency issues
                    if (currentGeofences.isNotEmpty()) {
                        val isOutside = isLocationOutsideAllGeofences(
                            location.latitude,
                            location.longitude,
                            currentGeofences
                        )
                        if (isOutside) {
                            Log.d("LocationMonitor", "Child is outside all geofence zones")
                            notifyParentChildOutsideZone(location.latitude, location.longitude)
                        }
                    }

                    // Update location in parent's data
                    if (user.parentLinkedId != null) {
                        val result = GeofenceRepository.updateChildLocationInParentData(
                            parentId = user.parentLinkedId!!,
                            childsActualId = user.getDecodedUid(),
                            locationData = ChildLocationData(
                                childId = user.getDecodedUid(),
                                avatarUrl = user.avatarUrl,
                                name = user.name,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                lastSeen = now
                            )
                        )
                        if (result.isSuccess) {
                            Log.d("LocationMonitor", "Location updated for ${user.getDecodedUid()}")
                        } else {
                            Log.e("LocationMonitor", "Failed to update location: ${result.exceptionOrNull()}")
                        }

                        // Always append to history (fire and forget), even if the latest doc update fails
                        val historyResult = GeofenceRepository.appendChildLocationHistory(
                            parentId = user.parentLinkedId!!,
                            childId = user.getDecodedUid(),
                            locationData = ChildLocationData(
                                childId = user.getDecodedUid(),
                                avatarUrl = user.avatarUrl,
                                name = user.name,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                lastSeen = now
                            )
                        )
                        if (historyResult.isSuccess) {
                            Log.d("LocationMonitor", "Appended location history for ${user.getDecodedUid()}")
                        } else {
                            Log.e("LocationMonitor", "Failed to append location history: ${historyResult.exceptionOrNull()}")
                        }
                    }
                }
            } ?: run {
                // Location is null - this might indicate location services are disabled
                Log.w("LocationMonitor", "Location is null, checking location services")
                if (!checkLocationServicesEnabled()) {
                    notifyChildLocationServicesDisabled()
                }
            }
        }.addOnFailureListener { e ->
            Log.e("LocationMonitor", "getCurrentLocation failed: ${e.message}")
            // Check if failure might be due to location services being disabled
            if (!checkLocationServicesEnabled()) {
                notifyChildLocationServicesDisabled()
            }
        }
    }
}