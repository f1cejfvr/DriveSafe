package com.rmas.drivesafe.ui.objects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
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
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var objects by remember { mutableStateOf<List<MapObject>>(emptyList()) }
    var filteredObjects by remember { mutableStateOf<List<MapObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedType by remember { mutableStateOf("SVE") }
    var searchQuery by remember { mutableStateOf("") }
    var radiusInput by remember { mutableStateOf("") }
    var useRadius by remember { mutableStateOf(false) }
    var selectedDateFilter by remember { mutableStateOf("SVE") }
    var filterByAuthor by remember { mutableStateOf(false) }
    var showTable by remember { mutableStateOf(false) }
    var selectedDangerLevel by remember { mutableIntStateOf(0) }
    var selectedServiceType by remember { mutableStateOf("SVE") }
    var selectedParkingType by remember { mutableStateOf("SVE") }

    val types = listOf("SVE", "EMERGENCY", "DANGER", "PARKING")
    val dateFilters = listOf("SVE", "Danas", "7 dana", "30 dana")
    val serviceTypes = listOf("SVE", "HOSPITAL", "POLICE", "FIRE", "CAR_SERVICE")
    val parkingTypes = listOf("SVE", "FREE", "PAID", "GARAGE")
    val dangerLevels = listOf(0, 1, 2, 3, 4, 5)

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

    LaunchedEffect(
        selectedType, searchQuery, useRadius, radiusInput, currentLocation,
        selectedDateFilter, filterByAuthor, selectedDangerLevel,
        selectedServiceType, selectedParkingType
    ) {
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
            val authorMatch = if (filterByAuthor) obj.authorId == currentUserId else true
            val attrMatch = when (obj.type) {
                "DANGER" -> selectedDangerLevel == 0 || obj.dangerLevel == selectedDangerLevel
                "EMERGENCY" -> selectedServiceType == "SVE" || obj.serviceType == selectedServiceType
                "PARKING" -> selectedParkingType == "SVE" || obj.parkingType == selectedParkingType
                else -> true
            }
            typeMatch && searchMatch && radiusMatch && dateMatch && authorMatch && attrMatch
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Objekti", fontSize = 24.sp)
            Row {
                TextButton(onClick = { showTable = false }) {
                    Text("Lista", color = if (!showTable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                TextButton(onClick = { showTable = true }) {
                    Text("Tabela", color = if (showTable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Samo moji objekti", fontSize = 14.sp)
            Switch(
                checked = filterByAuthor,
                onCheckedChange = { filterByAuthor = it }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
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
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
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

        if (selectedType == "DANGER") {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Nivo opasnosti:", fontSize = 12.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dangerLevels.forEach { level ->
                    FilterChip(
                        selected = selectedDangerLevel == level,
                        onClick = { selectedDangerLevel = level },
                        label = { Text(if (level == 0) "SVE" else level.toString(), fontSize = 10.sp) }
                    )
                }
            }
        }

        if (selectedType == "EMERGENCY") {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tip servisa:", fontSize = 12.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                serviceTypes.forEach { type ->
                    FilterChip(
                        selected = selectedServiceType == type,
                        onClick = { selectedServiceType = type },
                        label = { Text(type, fontSize = 10.sp) }
                    )
                }
            }
        }

        if (selectedType == "PARKING") {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tip parkinga:", fontSize = 12.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                parkingTypes.forEach { type ->
                    FilterChip(
                        selected = selectedParkingType == type,
                        onClick = { selectedParkingType = type },
                        label = { Text(type, fontSize = 10.sp) }
                    )
                }
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
        } else if (showTable) {
            ObjectTable(objects = filteredObjects, navController = navController)
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
fun ObjectTable(objects: List<MapObject>, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Naziv", fontWeight = FontWeight.Bold, modifier = Modifier.width(120.dp), fontSize = 12.sp)
            Text("Tip", fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp), fontSize = 12.sp)
            Text("Ocena", fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp), fontSize = 12.sp)
            Text("Autor", fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp), fontSize = 12.sp)
        }
        HorizontalDivider()
        LazyColumn {
            items(objects) { obj ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate(Screen.ObjectDetail.createRoute(obj.id))
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(obj.title, modifier = Modifier.width(120.dp), fontSize = 12.sp, maxLines = 1)
                    Text(obj.type, modifier = Modifier.width(80.dp), fontSize = 12.sp)
                    Text("%.1f".format(obj.rating), modifier = Modifier.width(60.dp), fontSize = 12.sp)
                    Text(obj.authorName, modifier = Modifier.width(100.dp), fontSize = 12.sp, maxLines = 1)
                }
                HorizontalDivider()
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
                text = "Ocena: ${"%.1f".format(obj.rating)} (${obj.ratingCount})",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}