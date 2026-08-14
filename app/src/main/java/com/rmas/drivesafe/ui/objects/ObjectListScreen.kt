package com.rmas.drivesafe.ui.objects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rmas.drivesafe.model.MapObject
import com.rmas.drivesafe.navigation.Screen
import com.rmas.drivesafe.repository.ObjectRepository
import com.rmas.drivesafe.service.LocationService
import com.rmas.drivesafe.ui.map.calculateDistance
import java.util.Calendar

@Composable
fun ObjectListScreen(navController: NavController) {
    val context = LocalContext.current
    val objectRepository = remember { ObjectRepository() }
    val locationService = remember { LocationService(context) }
    val currentLocation by locationService.currentLocation.collectAsState()

    var objects by remember { mutableStateOf<List<MapObject>>(emptyList()) }
    var filteredObjects by remember { mutableStateOf<List<MapObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedType by remember { mutableStateOf("SVE") }
    var searchQuery by remember { mutableStateOf("") }
    var radiusInput by remember { mutableStateOf("") }
    var useRadius by remember { mutableStateOf(false) }
    var selectedDateFilter by remember { mutableStateOf("SVE") }

    val types = listOf("SVE", "EMERGENCY", "DANGER", "PARKING")
    val dateFilters = listOf("SVE", "Danas", "7 dana", "30 dana")

    LaunchedEffect(Unit) {
        locationService.startTracking()
        val result = objectRepository.getAllObjects()
        if (result.isSuccess) {
            objects = result.getOrNull() ?: emptyList()
            filteredObjects = objects
        }
        isLoading = false
    }

    DisposableEffect(Unit) {
        onDispose { locationService.stopTracking() }
    }

    LaunchedEffect(selectedType, searchQuery, useRadius, radiusInput, currentLocation, selectedDateFilter) {
        filteredObjects = objects.filter { obj ->
            val typeMatch = selectedType == "SVE" || obj.type == selectedType
            val searchMatch = searchQuery.isEmpty() ||
                    obj.title.contains(searchQuery, ignoreCase = true) ||
                    obj.description.contains(searchQuery, ignoreCase = true)
            val radiusMatch = if (useRadius && currentLocation != null && radiusInput.isNotEmpty()) {
                val radius = radiusInput.toDoubleOrNull() ?: Double.MAX_VALUE
                val distance = calculateDistance(
                    currentLocation!!.latitude, currentLocation!!.longitude,
                    obj.latitude, obj.longitude
                )
                distance <= radius
            } else true
            val dateMatch = if (selectedDateFilter == "SVE" || obj.createdAt == null) {
                true
            } else {
                val calendar = Calendar.getInstance()
                when (selectedDateFilter) {
                    "Danas" -> calendar.add(Calendar.DAY_OF_YEAR, -1)
                    "7 dana" -> calendar.add(Calendar.DAY_OF_YEAR, -7)
                    "30 dana" -> calendar.add(Calendar.DAY_OF_YEAR, -30)
                }
                obj.createdAt.after(calendar.time)
            }
            typeMatch && searchMatch && radiusMatch && dateMatch
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Objekti",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Pretrazi") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = radiusInput,
                onValueChange = { radiusInput = it },
                label = { Text("Radijus (m)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = useRadius,
                onCheckedChange = { useRadius = it }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            types.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { selectedType = type },
                    label = { Text(type, fontSize = 10.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dateFilters.forEach { filter ->
                FilterChip(
                    selected = selectedDateFilter == filter,
                    onClick = { selectedDateFilter = filter },
                    label = { Text(filter, fontSize = 10.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (filteredObjects.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nema objekata")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredObjects) { obj ->
                    ObjectCard(obj = obj, onClick = {
                        navController.navigate(Screen.ObjectDetail.createRoute(obj.id))
                    })
                }
            }
        }
    }
}

@Composable
fun ObjectCard(obj: MapObject, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = obj.title,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = obj.type,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (obj.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = obj.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ocena: ${String.format(java.util.Locale.getDefault(), "%.1f", obj.rating)} (${obj.ratingCount})",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}