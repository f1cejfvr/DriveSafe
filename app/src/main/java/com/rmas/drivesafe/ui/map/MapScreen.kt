package com.rmas.drivesafe.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.rmas.drivesafe.model.MapObject
import com.rmas.drivesafe.repository.ObjectRepository
import com.rmas.drivesafe.service.LocationService
import com.rmas.drivesafe.service.NotificationService
import kotlin.math.*

fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000.0
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dphi = Math.toRadians(lat2 - lat1)
    val dlambda = Math.toRadians(lon2 - lon1)
    val a = sin(dphi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dlambda / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

@Composable
fun MapScreen(navController: NavController) {
    val context = LocalContext.current
    val locationService = remember { LocationService(context) }
    val objectRepository = remember { ObjectRepository() }
    val currentLocation by locationService.currentLocation.collectAsState()

    var objects by remember { mutableStateOf<List<MapObject>>(emptyList()) }
    var notifiedObjects by remember { mutableStateOf<Set<String>>(emptySet()) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (hasLocationPermission) {
            locationService.startTracking()
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            locationService.startTracking()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        val result = objectRepository.getAllObjects()
        if (result.isSuccess) {
            objects = result.getOrNull() ?: emptyList()
        }
    }

    LaunchedEffect(currentLocation) {
        currentLocation?.let { location ->
            objects.forEach { obj ->
                val distance = calculateDistance(
                    location.latitude, location.longitude,
                    obj.latitude, obj.longitude
                )
                if (distance < 200 && !notifiedObjects.contains(obj.id)) {
                    NotificationService.showLocalNotification(
                        context,
                        "Objekat u blizini!",
                        "${obj.title} je na ${distance.toInt()}m od vas"
                    )
                    notifiedObjects = notifiedObjects + obj.id
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            locationService.stopTracking()
        }
    }

    val defaultLocation = LatLng(43.3209, 21.8954)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            currentLocation ?: defaultLocation, 12f
        )
    }

    LaunchedEffect(currentLocation) {
        currentLocation?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 15f)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = hasLocationPermission
            )
        ) {
            currentLocation?.let {
                val markerState = rememberUpdatedMarkerState(position = it)
                Marker(
                    state = markerState,
                    title = "Moja lokacija"
                )
            }
            objects.forEach { obj ->
                val markerState = rememberUpdatedMarkerState(
                    position = LatLng(obj.latitude, obj.longitude)
                )
                Marker(
                    state = markerState,
                    title = obj.title,
                    snippet = obj.type
                )
            }
        }

        FloatingActionButton(
            onClick = {
                val lat = currentLocation?.latitude ?: 43.3209
                val lng = currentLocation?.longitude ?: 21.8954
                navController.navigate("add_object?lat=$lat&lng=$lng")
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("+")
        }
    }
}