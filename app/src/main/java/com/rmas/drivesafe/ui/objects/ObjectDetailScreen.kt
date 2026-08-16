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
import com.rmas.drivesafe.model.Rating
import com.rmas.drivesafe.repository.ObjectRepository
import com.rmas.drivesafe.repository.UserRepository
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun ObjectDetailScreen(
    navController: NavController,
    objectId: String
) {
    val objectRepository = remember { ObjectRepository() }
    val userRepository = remember { UserRepository() }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val scope = rememberCoroutineScope()

    var mapObject by remember { mutableStateOf<MapObject?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var userRating by remember { mutableFloatStateOf(0f) }
    var comment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(objectId) {
        val result = objectRepository.getObject(objectId)
        if (result.isSuccess) {
            mapObject = result.getOrNull()
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val obj = mapObject ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = obj.title, fontSize = 24.sp, modifier = Modifier.weight(1f))
            Text(
                text = obj.type,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Autor: ${obj.authorName}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (obj.description.isNotEmpty()) {
            Text(text = obj.description, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        when (obj.type) {
            "EMERGENCY" -> {
                if (obj.phone.isNotEmpty()) {
                    Text("Telefon: ${obj.phone}", fontSize = 14.sp)
                }
                if (obj.workingHours.isNotEmpty()) {
                    Text("Radno vreme: ${obj.workingHours}", fontSize = 14.sp)
                }
            }
            "DANGER" -> {
                Text("Nivo opasnosti: ${obj.dangerLevel}/5", fontSize = 14.sp)
                Text("Tip: ${obj.dangerType}", fontSize = 14.sp)
            }
            "PARKING" -> {
                Text("Broj mesta: ${obj.totalSpots}", fontSize = 14.sp)
                if (obj.pricePerHour > 0) {
                    Text("Cena: ${obj.pricePerHour} RSD/h", fontSize = 14.sp)
                } else {
                    Text("Besplatno parkiranje", fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Prosecna ocena: ${"%.1f".format(obj.rating)} (${obj.ratingCount} ocena)",
            fontSize = 14.sp
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Ostavi ocenu:", fontSize = 16.sp)

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { star ->
                FilterChip(
                    selected = userRating >= star,
                    onClick = { userRating = star.toFloat() },
                    label = { Text("$star") }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Komentar") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (successMessage.isNotEmpty()) {
            Text(text = successMessage, color = MaterialTheme.colorScheme.primary)
        }
        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (userRating == 0f) {
                    errorMessage = "Izaberite ocenu"
                    return@Button
                }
                scope.launch {
                    isSubmitting = true
                    val rating = Rating(
                        objectId = objectId,
                        userId = currentUser?.uid ?: "",
                        rating = userRating,
                        comment = comment,
                        createdAt = Date()
                    )
                    val result = objectRepository.addRating(rating)
                    if (result.isSuccess) {
                        val pointsToAdd = if (comment.isNotBlank()) 10 else 5
                        userRepository.updateUserPoints(currentUser?.uid ?: "", pointsToAdd)
                        successMessage = if (comment.isNotBlank())
                            "Ocena i komentar uspesno dodati! +10 poena"
                        else
                            "Ocena uspesno dodata! +5 poena"
                        errorMessage = ""
                        val updatedObj = objectRepository.getObject(objectId)
                        if (updatedObj.isSuccess) {
                            mapObject = updatedObj.getOrNull()
                        }
                    } else {
                        errorMessage = result.exceptionOrNull()?.message ?: "Greska"
                    }
                    isSubmitting = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSubmitting
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Posalji ocenu")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Nazad")
        }
    }
}