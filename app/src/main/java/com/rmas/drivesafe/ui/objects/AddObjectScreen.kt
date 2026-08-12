package com.rmas.drivesafe.ui.objects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.rmas.drivesafe.model.MapObject
import com.rmas.drivesafe.repository.ObjectRepository
import com.rmas.drivesafe.repository.UserRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddObjectScreen(
    navController: NavController,
    latitude: Double = 0.0,
    longitude: Double = 0.0
) {
    val scope = rememberCoroutineScope()
    val objectRepository = remember { ObjectRepository() }
    val userRepository = remember { UserRepository() }
    val currentUser = FirebaseAuth.getInstance().currentUser

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("EMERGENCY") }
    var serviceType by remember { mutableStateOf("HOSPITAL") }
    var dangerType by remember { mutableStateOf("POTHOLE") }
    var parkingType by remember { mutableStateOf("FREE") }
    var phone by remember { mutableStateOf("") }
    var workingHours by remember { mutableStateOf("") }
    var totalSpots by remember { mutableStateOf("") }
    var pricePerHour by remember { mutableStateOf("") }
    var dangerLevel by remember { mutableStateOf(1f) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val objectTypes = listOf("EMERGENCY", "DANGER", "PARKING")
    var typeExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Dodaj objekat",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Naziv") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Opis") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = typeExpanded,
            onExpandedChange = { typeExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Tip objekta") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false }
            ) {
                objectTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            selectedType = type
                            typeExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedType) {
            "EMERGENCY" -> {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefon") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = workingHours,
                    onValueChange = { workingHours = it },
                    label = { Text("Radno vreme") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "DANGER" -> {
                Text("Nivo opasnosti: ${dangerLevel.toInt()}")
                Slider(
                    value = dangerLevel,
                    onValueChange = { dangerLevel = it },
                    valueRange = 1f..5f,
                    steps = 3
                )
            }
            "PARKING" -> {
                OutlinedTextField(
                    value = totalSpots,
                    onValueChange = { totalSpots = it },
                    label = { Text("Broj mesta") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pricePerHour,
                    onValueChange = { pricePerHour = it },
                    label = { Text("Cena po satu") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (title.isBlank()) {
                    errorMessage = "Unesite naziv"
                    return@Button
                }
                scope.launch {
                    isLoading = true
                    val mapObject = MapObject(
                        type = selectedType,
                        title = title,
                        description = description,
                        latitude = latitude,
                        longitude = longitude,
                        authorId = currentUser?.uid ?: "",
                        authorName = currentUser?.email ?: "",
                        imageUrl = "",
                        serviceType = if (selectedType == "EMERGENCY") serviceType else "",
                        phone = if (selectedType == "EMERGENCY") phone else "",
                        workingHours = if (selectedType == "EMERGENCY") workingHours else "",
                        dangerType = if (selectedType == "DANGER") dangerType else "",
                        dangerLevel = if (selectedType == "DANGER") dangerLevel.toInt() else 0,
                        parkingType = if (selectedType == "PARKING") parkingType else "",
                        totalSpots = if (selectedType == "PARKING") totalSpots.toIntOrNull() ?: 0 else 0,
                        pricePerHour = if (selectedType == "PARKING") pricePerHour.toDoubleOrNull() ?: 0.0 else 0.0
                    )
                    val result = objectRepository.addObject(mapObject)
                    if (result.isSuccess) {
                        userRepository.updateUserPoints(currentUser?.uid ?: "", 10)
                        navController.popBackStack()
                    } else {
                        errorMessage = result.exceptionOrNull()?.message ?: "Greska"
                    }
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Sacuvaj")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Otkazi")
        }
    }
}