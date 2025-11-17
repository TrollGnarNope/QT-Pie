package com.veigar.questtracker.services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.veigar.questtracker.data.GeofenceRepository
import com.veigar.questtracker.model.UserModel
import com.veigar.questtracker.services.receiver.GeofenceReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceMonitor(
    private val context: Context,
    private val child: UserModel,
    private val serviceScope: CoroutineScope
) {
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
    // private val addedGeofenceIds = mutableSetOf<String>() // <-- REMOVED: This was causing the bug

    private var isAdding = false

    private fun getGeofencePendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceReceiver::class.java).apply {
            action = "com.veigar.questtracker.ACTION_GEOFENCE"
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            flags
        )
    }
    fun clearAllGeofences() {
        geofencingClient.removeGeofences(getGeofencePendingIntent())
            .addOnSuccessListener {
                Log.i("GeofenceManager", "All geofences removed.")
                // addedGeofenceIds.clear() // <-- REMOVED
            }
            .addOnFailureListener { e ->
                Log.e("GeofenceManager", "Failed to remove geofences: ${e.message}")
            }
    }

    fun loadAndRegisterGeofences() {
        if(isAdding){
            println("Added already")
            return
        }
        clearAllGeofences()
        isAdding = true
        serviceScope.launch(Dispatchers.IO) {
            GeofenceRepository.getGeofenceUpdates(child.parentLinkedId!!).collect { geofences ->
                geofences.fold(
                    onSuccess = {
                        it.forEach { geofence ->
                            addGeofence(
                                geofence.geoId,
                                geofence.latitude,
                                geofence.longitude,
                                geofence.radius.toFloat(),
                                geofence.name
                            )
                        }
                    },
                    onFailure = {
                        Log.e("GeofenceManager", "Failed to load geofences: ${it.message}")
                    }
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun addGeofence(id: String, lat: Double, lng: Double, radius: Float, name: String) {
        // if(addedGeofenceIds.contains(name)){ // <-- REMOVED: This check prevented updates
        //     println("Geofence already added: $name")
        //     return
        // }
        val requestId = "${name}|${child.parentLinkedId}|${child.name}"
        val geofence = Geofence.Builder()
            .setRequestId(requestId)
            .setCircularRegion(lat, lng, radius)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, getGeofencePendingIntent())
            .addOnSuccessListener {
                // addedGeofenceIds.add(name) // <-- REMOVED
                Log.i("GeofenceManager", "Added/Updated geofence: $name") // (Log message updated for clarity)
            }
            .addOnFailureListener { e -> Log.e("GeofenceManager", "Failed to add geofence: ${e.message} $requestId") }
    }
}