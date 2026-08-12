package com.rmas.drivesafe.ui.objects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rmas.drivesafe.model.MapObject
import com.rmas.drivesafe.navigation.Screen
import com.rmas.drivesafe.repository.ObjectRepository

@Composable
fun ObjectListScreen(navController: NavController) {
    val objectRepository = remember { ObjectRepository() }
    var objects by remember { mutableStateOf<List<MapObject>>(emptyList()) }
    var filteredObjects by remember { mutableStateOf<List<MapObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedType by remember { mutableStateOf("SVE") }
    var searchQuery by remember { mutableStateOf("") }

    val types = listOf("SVE", "EMERGENCY", "DANGER", "PARKING")

    LaunchedEffect(Unit) {
        val result = objectRepository.getAllObjects()
        if (result.isSuccess) {
            objects = result.getOrNull() ?: emptyList()
            filteredObjects = objects
        }
        isLoading = false
    }

    LaunchedEffect(selectedType, searchQuery) {
        filteredObjects = objects.filter { obj ->
            val typeMatch = selectedType == "SVE" || obj.type == selectedType
            val searchMatch = searchQuery.isEmpty() ||
                    obj.title.contains(searchQuery, ignoreCase = true) ||
                    obj.description.contains(searchQuery, ignoreCase = true)
            typeMatch && searchMatch
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
                text = "Ocena: ${String.format(java.util.Locale.getDefault(),"%.1f", obj.rating)} (${obj.ratingCount})",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}