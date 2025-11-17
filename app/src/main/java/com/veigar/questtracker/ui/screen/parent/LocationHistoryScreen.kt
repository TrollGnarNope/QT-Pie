package com.veigar.questtracker.ui.screen.parent

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Place
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.veigar.questtracker.data.GeofenceRepository
import com.veigar.questtracker.model.ChildLocationData
import com.veigar.questtracker.model.GeofenceData
import com.veigar.questtracker.ui.component.DisplayAvatar
import com.veigar.questtracker.ui.theme.CoralBlueDark
import com.veigar.questtracker.ui.theme.ProfessionalGray
import com.veigar.questtracker.ui.theme.ProfessionalGrayDark
import com.veigar.questtracker.ui.theme.ProfessionalGrayText
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationHistoryScreen(
    navController: NavController,
    parentId: String,
    childId: String
) {
    val context = LocalContext.current
    val flow = GeofenceRepository.observeChildLocationHistory(parentId, childId)
    val items: List<ChildLocationData> by flow.map { it }.collectAsState(initial = emptyList())

    val geofences: List<GeofenceData> = produceState(initialValue = emptyList(), key1 = parentId) {
        val res = GeofenceRepository.getAllGeofences(parentId)
        value = res.getOrElse { emptyList() }
    }.value

    fun computeNearestStatusLikeParent(lat: Double, lng: Double): String {
        if (geofences.isEmpty()) return "Lat: ${String.format("%.4f", lat)}, Lng: ${String.format("%.4f", lng)}"
        var closestStatus: String? = null
        var minDistanceToCenterForStatus = Float.MAX_VALUE
        geofences.forEach { geofence ->
            val geofenceRadius = geofence.radius.toFloat()
            val nearRadius = geofenceRadius * 2f
            val distanceToCenter = distanceMeters(lat, lng, geofence.latitude, geofence.longitude)
            if (distanceToCenter <= geofenceRadius) {
                if (closestStatus == null || !closestStatus!!.startsWith("Inside") || distanceToCenter < minDistanceToCenterForStatus) {
                    closestStatus = "Inside ${geofence.name}"
                    minDistanceToCenterForStatus = distanceToCenter
                }
            } else if (distanceToCenter <= nearRadius) {
                if (closestStatus == null || (closestStatus!!.startsWith("Near") && distanceToCenter < minDistanceToCenterForStatus)) {
                    closestStatus = "Near ${geofence.name}"
                    minDistanceToCenterForStatus = distanceToCenter
                }
            }
        }
        return closestStatus ?: "Lat: ${String.format("%.4f", lat)}, Lng: ${String.format("%.4f", lng)}"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Location History") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ProfessionalGray, titleContentColor = ProfessionalGrayText),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ProfessionalGrayText)
                    }
                }
            )
        },
        containerColor = ProfessionalGrayDark
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (items.isEmpty()) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOff,
                            contentDescription = "Empty",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No location history yet",
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "It will appear once the child shares a location",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = items,
                        key = { loc -> loc.documentId.ifBlank { "${loc.lastSeen}_${String.format("%.6f", loc.latitude)}_${String.format("%.6f", loc.longitude)}" } }
                    ) { loc ->
                        val time = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(loc.lastSeen))
                        Surface(
                            color = Color(0x33000000),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp)) {
                                DisplayAvatar(fullAssetPath = loc.avatarUrl, size = 40.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = loc.name.ifBlank { "Unknown Child" }, color = Color.White)
                                    val status = computeNearestStatusLikeParent(loc.latitude, loc.longitude)
                                    if (status.startsWith("Inside") || status.startsWith("Near")) {
                                        Surface(
                                            color = Color(0x33FFD54F),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = status,
                                                color = Color(0xFFFFDA63),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        val coordinatesRegex = Regex("Lat: ([-\\d.]+), Lng: ([-\\d.]+)")
                                        val matchResult = coordinatesRegex.find(status)
                                        
                                        Row(
                                            modifier = if (matchResult != null) {
                                                Modifier.clickable {
                                                    val lat = matchResult.groupValues[1]
                                                    val lng = matchResult.groupValues[2]
                                                    openLocationInMaps(context, lat.toDouble(), lng.toDouble())
                                                }
                                            } else {
                                                Modifier
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Place,
                                                contentDescription = "Coordinates",
                                                tint = Color.White.copy(alpha = 0.75f)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = status,
                                                color = Color.White.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                    Text(text = time, color = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val R = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return (R * c).toFloat()
}

private fun openLocationInMaps(context: android.content.Context, latitude: Double, longitude: Double) {
    val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
    mapIntent.setPackage("com.google.android.apps.maps")
    
    // Check if Google Maps is available
    if (mapIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(mapIntent)
    } else {
        // Fallback to any map app
        val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
        if (fallbackIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(fallbackIntent)
        }
    }
}


