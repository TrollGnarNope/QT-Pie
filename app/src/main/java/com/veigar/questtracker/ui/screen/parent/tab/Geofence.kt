package com.veigar.questtracker.ui.screen.parent.tab

import android.Manifest
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.DragState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.veigar.questtracker.model.ChildLocationData
import com.veigar.questtracker.model.GeofenceData
import com.veigar.questtracker.ui.component.isNetworkOrCustomPath
import com.veigar.questtracker.ui.theme.ProfessionalGrayDark
import com.veigar.questtracker.ui.theme.yellow
import com.veigar.questtracker.util.ImageManager
import com.veigar.questtracker.viewmodel.ParentDashboardViewModel
import androidx.compose.runtime.rememberUpdatedState

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalPermissionsApi::class, MapsComposeExperimentalApi::class)
@Composable
fun GeofenceTab(
    geofenceViewModel: ParentDashboardViewModel
) {
    val application = LocalContext.current

    val uiState by geofenceViewModel.uiState.collectAsStateWithLifecycle()
    val focusedChildId by geofenceViewModel.focusedChildId.collectAsStateWithLifecycle()
    val currentContext = LocalContext.current

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    // --- Location Permissions Handling (using Accompanist) ---
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ),
        onPermissionsResult = { permissionsResult ->
            val allGranted = permissionsResult.all { it.value }
            geofenceViewModel.onPermissionResult(
                allGranted,
                fusedLocationClient = fusedLocationClient,
                context = application,
            )
        }
    )

    // Effect to launch permission request if needed
    LaunchedEffect(Unit) {
        if (!uiState.permissionGranted && !locationPermissionsState.allPermissionsGranted) {
            locationPermissionsState.launchMultiplePermissionRequest()
        } else if (!uiState.permissionGranted && locationPermissionsState.allPermissionsGranted) {
            geofenceViewModel.onPermissionResult(true, fusedLocationClient = fusedLocationClient,
                context = application)
        }
    }

    // Handle Snackbar messages (e.g., by showing a Toast if no higher-level Scaffold)
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            Toast.makeText(currentContext, message, Toast.LENGTH_LONG).show()
            geofenceViewModel.consumeSnackbarMessage() // Clear message after showing
        }
    }

    Box(
        modifier = Modifier.fillMaxSize() // This Box is now the root of the tab's UI
    ) {
        when {
            // Case 1: System permissions not granted
            !locationPermissionsState.allPermissionsGranted -> {
                PermissionRequestUI(
                    onGrantPermissionsClick = {
                        locationPermissionsState.launchMultiplePermissionRequest()
                    },
                    // You might want to pass locationPermissionsState.shouldShowRationale here
                    // to customize the text if rationale is needed.
                    shouldShowRationale = locationPermissionsState.shouldShowRationale
                )
            }
            // Case 2: Permissions considered granted by ViewModel, but map not ready to be shown
            // (either location not fetched or user interaction needed)
            uiState.permissionGranted && !uiState.showMap -> {
                GetLocationUI(
                    isLoading = uiState.isFetchingLocation,
                    onGetLocationClick = {
                        geofenceViewModel.fetchDeviceLocation(
                            fusedLocationClient = fusedLocationClient,
                            context = application,
                        )
                    }
                )
            }
            // Case 3: Permissions granted and ready to show map
            uiState.permissionGranted && uiState.deviceLocation != null -> {
                MapContainer(
                    deviceLocation = uiState.deviceLocation!!,
                    geofences = uiState.geofences,
                    selectedGeofenceId = uiState.selectedGeofenceId, // Pass selected ID
                    onGeofenceClick = { geofenceId -> // New callback for marker click
                        geofenceViewModel.onGeofenceMarkerClick(geofenceId)
                    },
                    onMapClick = { // New callback for map click to deselect
                        geofenceViewModel.deselectGeofence()
                    },
                    onPrepareToAddGeofence = { currentMapCenter ->
                        geofenceViewModel.prepareToAddGeofenceAt(currentMapCenter)
                    },
                    onEditGeofenceClick = { // New callback for edit button
                        geofenceViewModel.prepareToEditSelectedGeofence()
                    },
                    onGeofenceMoved = { geofenceId, newPosition -> // New Callback
                        geofenceViewModel.updateGeofencePosition(geofenceId, newPosition)
                    },
                    onMapLoaded = { geofenceViewModel.onMapLoaded() },
                    isMapInitializationComplete = uiState.mapInitializationComplete,
                    childLocations = uiState.childLocations,
                    onRefreshClick = {
                        geofenceViewModel.requestLocationUpdate()
                    },
                    focusedChildId = focusedChildId
                )
            }
            // Fallback / Initial state if permissions are granted (by ViewModel)
            // but still figuring things out (e.g. location not yet fetched or currently fetching)
            uiState.permissionGranted -> {
                GetLocationUI(
                    isLoading = uiState.isFetchingLocation,
                    onGetLocationClick = {
                        geofenceViewModel.fetchDeviceLocation(fusedLocationClient = fusedLocationClient,
                            context = application)
                    }
                )
            }
            // Default fallback if none of the above specific states match (e.g., permissions not granted by ViewModel state)
            else -> {
                PermissionRequestUI(
                    onGrantPermissionsClick = {
                        locationPermissionsState.launchMultiplePermissionRequest()
                    },
                    shouldShowRationale = locationPermissionsState.shouldShowRationale
                )
            }
        }
    }
    // --- Dialogs ---
    if (uiState.showAddGeofenceDialog) {
        AddOrEditGeofenceDialog(
            dialogTitle = "Add Geofence Details",
            confirmButtonText = "Add",
            initialName = "", // Empty for new geofence
            initialRadius = "", // Empty for new geofence
            onDismissRequest = { geofenceViewModel.dismissAddGeofenceDialog() },
            onConfirm = { name, radius ->
                geofenceViewModel.saveNewGeofence(name, radius)
            }
        )
    }

    if (uiState.showEditGeofenceDialog) {
        val selectedGeofence = uiState.geofences.find { it.geoId == uiState.selectedGeofenceId }
        selectedGeofence?.let {
            AddOrEditGeofenceDialog(
                dialogTitle = "Edit Geofence Details",
                confirmButtonText = "Save Changes",
                initialName = it.name,
                initialRadius = it.radius.toString(),
                onDismissRequest = { geofenceViewModel.dismissEditGeofenceDialog() },
                onConfirm = { name, radius ->
                    geofenceViewModel.saveEditedGeofence(name, radius)
                },
                onRemove = {
                    geofenceViewModel.removeSelectedGeofence()
                }
            )
        }
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun MapContainer(
    deviceLocation: LatLng,
    geofences: List<GeofenceData>,
    selectedGeofenceId: String?,
    onGeofenceClick: (String) -> Unit,
    onMapClick: () -> Unit,
    onPrepareToAddGeofence: (LatLng) -> Unit,
    onEditGeofenceClick: () -> Unit,
    onGeofenceMoved: (geofenceId: String, newPosition: LatLng) -> Unit,
    onMapLoaded: () -> Unit,
    isMapInitializationComplete: Boolean,
    childLocations: List<ChildLocationData>,
    onRefreshClick: () -> Unit,
    focusedChildId: String?
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(deviceLocation, 2f)
    }

    LaunchedEffect(deviceLocation, isMapInitializationComplete) {
        if (isMapInitializationComplete) {
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition(deviceLocation, 15f, 0f, 0f)
                ),
                1000
            )
        }
    }

    LaunchedEffect(focusedChildId) {
        if (focusedChildId != null) {
            val childLocation = childLocations.find { it.childId == focusedChildId }?.position
            if (childLocation != null) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition(childLocation, 17f, 0f, 0f)
                    ),
                    1000
                )
            }
        }
    }

    var currentlyDraggedGeofenceId by remember { mutableStateOf<String?>(null) }
    var currentDragPosition by remember { mutableStateOf<LatLng?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.matchParentSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = true),
            onMapLoaded = onMapLoaded,
            onMapClick = { latLng ->
                onMapClick()
                currentlyDraggedGeofenceId = null
                currentDragPosition = null
            },
        ) {
            geofences.forEach { geofence ->
                val markerState = rememberUpdatedMarkerState(key = geofence.geoId, position = geofence.position)

                LaunchedEffect(geofence.position) {
                    if (markerState.position != geofence.position) {
                        markerState.position = geofence.position
                    }
                }

                val isCurrentlyBeingDragged = markerState.dragState == DragState.DRAG

                val circlePosition = when {
                    geofence.geoId == currentlyDraggedGeofenceId && currentDragPosition != null -> currentDragPosition!!
                    else -> geofence.position
                }

                Marker(
                    state = markerState,
                    title = geofence.name,
                    snippet = "Radius: ${geofence.radius}m ${if (geofence.geoId == selectedGeofenceId) "(Selected)" else ""}",
                    icon = BitmapDescriptorFactory.defaultMarker(
                        if (geofence.geoId == selectedGeofenceId || isCurrentlyBeingDragged) BitmapDescriptorFactory.HUE_ORANGE else BitmapDescriptorFactory.HUE_GREEN
                    ),
                    tag = geofence.geoId,
                    draggable = true,
                    onClick = { clickedMarker ->
                        val id = clickedMarker.tag as? String
                        if (id != null) {
                            onGeofenceClick(id)
                        }
                        false
                    }
                )

                LaunchedEffect(markerState.dragState) {
                    when (markerState.dragState) {
                        DragState.START -> {
                            currentlyDraggedGeofenceId = geofence.geoId
                            currentDragPosition = markerState.position
                            onGeofenceClick(geofence.geoId)
                        }
                        DragState.DRAG -> {
                            if (currentlyDraggedGeofenceId == geofence.geoId) {
                                currentDragPosition = markerState.position
                            }
                        }
                        DragState.END -> {
                            if (currentlyDraggedGeofenceId == geofence.geoId) {
                                onGeofenceMoved(geofence.geoId, markerState.position)
                                currentlyDraggedGeofenceId = null
                                currentDragPosition = null
                            }
                        }
                    }
                }

                Circle(
                    center = circlePosition,
                    radius = geofence.radius,
                    strokeColor = if (geofence.geoId == selectedGeofenceId || isCurrentlyBeingDragged) yellow.copy(alpha = 0.8f) else Color.Green.copy(alpha = 0.6f),
                    fillColor = if (geofence.geoId == selectedGeofenceId || isCurrentlyBeingDragged) yellow.copy(alpha = 0.4f) else Color.Green.copy(alpha = 0.3f),
                    strokeWidth = if (geofence.geoId == selectedGeofenceId || isCurrentlyBeingDragged) 5f else 3f,
                    zIndex = if (geofence.geoId == selectedGeofenceId || isCurrentlyBeingDragged) 1f else 0.5f,
                    clickable = true,
                    onClick = {
                        if (currentlyDraggedGeofenceId != geofence.geoId) {
                            onGeofenceClick(geofence.geoId)
                        }
                    }
                )
            }

            childLocations.forEach { childLocation ->
                CustomMarker(childLocation = childLocation, onClick = {
                    onMapClick()
                })
            }
        }

        FloatingActionButton(
            onClick = onRefreshClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(CircleShape),
            containerColor = yellow
        ) {
            Icon(Icons.Filled.Refresh, "Refresh Location", tint = Color.White)
        }

        Icon(
            imageVector = Icons.Filled.MyLocation,
            contentDescription = "Map Center Target",
            modifier = Modifier.align(Alignment.Center),
            tint = Color.Black.copy(alpha = 0.7f)
        )

        if (!isMapInitializationComplete) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    val currentMapCenter = cameraPositionState.position.target
                    onPrepareToAddGeofence(currentMapCenter)
                },
                modifier = Modifier.clip(CircleShape),
                containerColor = yellow
            ) {
                Icon(Icons.Filled.AddLocation, "Add Geofence at Center", tint = Color.White)
            }

            if (selectedGeofenceId != null) {
                FloatingActionButton(
                    onClick = onEditGeofenceClick,
                    modifier = Modifier.clip(CircleShape),
                    containerColor = yellow
                ) {
                    Icon(Icons.Filled.Edit, "Edit Selected Geofence", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun CustomMarker(
    childLocation: ChildLocationData,
    onClick: () -> Unit,
) {
    val markerState = rememberUpdatedMarkerState(position = childLocation.position)
    val infoWindowState = remember { MarkerState(position = childLocation.position) }
    var imageLoaded by remember { mutableStateOf(false) }
    var isSelected by remember { mutableStateOf(false) }
    val link = if (isNetworkOrCustomPath(childLocation.avatarUrl)) {
        ImageManager.getFullUrl(childLocation.avatarUrl)
    } else {
        "file:///android_asset/${childLocation.avatarUrl}"
    }
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(link)
            .allowHardware(false)
            .build(),
        onSuccess = {
            imageLoaded = true
        },
        onError = {
            imageLoaded = true
        }
    )

    val currentOnClick by rememberUpdatedState(onClick)

    MarkerComposable(
        keys = arrayOf(childLocation.avatarUrl, imageLoaded),
        state = markerState,
        title = childLocation.name,
        anchor = Offset(0.5f, 1f),
        onClick = {
            isSelected = !isSelected
            infoWindowState.showInfoWindow()
            currentOnClick()
            true
        },
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ProfessionalGrayDark)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (childLocation.avatarUrl.isNotEmpty()) {
                Log.d("CustomMarker", "Displaying avatar for ${childLocation.avatarUrl}")
                Image(
                    painter = painter,
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = childLocation.name,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    if (isSelected) {
        MarkerInfoWindow(
            state = infoWindowState,
            onInfoWindowClose = {
                isSelected = false
            },
        ) {
            Column(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(childLocation.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Last seen: ${formatTimeAgo(childLocation.lastSeen)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}


@Composable
fun AddOrEditGeofenceDialog(
    dialogTitle: String,
    confirmButtonText: String,
    initialName: String,
    initialRadius: String,
    onDismissRequest: () -> Unit,
    onConfirm: (name: String, radius: String) -> Unit,
    onRemove: (() -> Unit)? = null
) {
    var geofenceName by remember(initialName) { mutableStateOf(initialName) }
    var geofenceRadius by remember(initialRadius) { mutableStateOf(initialRadius) }

    LaunchedEffect(initialName, initialRadius) {
        geofenceName = initialName
        geofenceRadius = initialRadius
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(dialogTitle) },
        text = {
            Column {
                OutlinedTextField(
                    value = geofenceName,
                    onValueChange = { geofenceName = it },
                    label = { Text("Geofence Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = geofenceRadius,
                    onValueChange = { geofenceRadius = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Radius (meters)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(geofenceName, geofenceRadius)
            }) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            Row {
                if (onRemove != null) {
                    TextButton(onClick = onRemove) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun PermissionRequestUI(
    onGrantPermissionsClick: () -> Unit,
    shouldShowRationale: Boolean // Added to customize message
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (shouldShowRationale) {
                "To show your location on the map and use geofence features, we need access to your device's location. Please grant the permission."
            } else {
                "Location permission is needed to show your current location on the map and set up geofences."
            },
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(onClick = onGrantPermissionsClick) {
            Text("Grant Permissions")
        }
    }
}

@Composable
fun formatTimeAgo(timeMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timeMillis

    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
        hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
        minutes > 0 -> "$minutes min${if (minutes > 1) "s" else ""} ago"
        seconds > 0 -> "$seconds sec${if (seconds > 1) "s" else ""} ago"
        else -> "Just now"
    }
}


@Composable
fun GetLocationUI(isLoading: Boolean, onGetLocationClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "We need your location to show it on the map.",
            color = Color.White,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(bottom = 16.dp),
                color = Color.White
            )
            Text(
                "Fetching location...",
                color = Color.White
            )
        } else {
            Button(
                onClick = onGetLocationClick,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text("Get My Current Location", color = Color.White)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Make sure your device's location services (GPS) are enabled for accurate results.",
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}
