package com.veigar.questtracker.model

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class GeofenceData(
    @DocumentId
    val geoId: String = UUID.randomUUID().toString(),
    // Store as separate latitude and longitude for Firestore
    val latitude: Double = 0.0, // Default values
    val longitude: Double = 0.0,
    val name: String = "",
    val radius: Double = 0.0
) {
    // Computed property to easily get LatLng
    // @get:Exclude ensures Firestore doesn't try to serialize/deserialize this getter
    @get:Exclude
    val position: LatLng
        get() = LatLng(latitude, longitude)

    // Convenience constructor if you often create it with a LatLng object
    constructor(
        geoId: String = UUID.randomUUID().toString(),
        latLng: LatLng, // Take LatLng here
        name: String,
        radius: Double
    ) : this(
        geoId,
        latLng.latitude, // Store individual components
        latLng.longitude,
        name,
        radius
    )
}

@Serializable
data class ChildLocationData(
    @DocumentId
    val documentId: String = "",
    val childId: String = "",
    val avatarUrl: String = "",
    val name: String = "",

    // Store latitude and longitude as separate Double fields for Firestore
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,

    // Client-set timestamp
    val lastSeen: Long = System.currentTimeMillis()
) {
    // Computed property to easily get LatLng in your app code
    @get:Exclude
    val position: LatLng
        get() = LatLng(latitude, longitude)

    // Convenience constructor if you often create it with a LatLng object
    // and want to explicitly set the timestamp (or let it default)
    constructor(
        childId: String,
        avatarUrl: String,
        name: String,
        latLng: LatLng,
        lastSeenMillis: Long = System.currentTimeMillis()
    ) : this(
        childId = childId,
        avatarUrl = avatarUrl,
        name = name,
        latitude = latLng.latitude,
        longitude = latLng.longitude,
        lastSeen = lastSeenMillis
    )
}