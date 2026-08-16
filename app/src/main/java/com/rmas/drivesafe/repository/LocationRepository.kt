package com.rmas.drivesafe.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LocationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val locationsCollection = db.collection("locations")
    private val auth = FirebaseAuth.getInstance()

    suspend fun updateUserLocation(latitude: Double, longitude: Double): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            val locationData = mapOf(
                "userId" to userId,
                "latitude" to latitude,
                "longitude" to longitude,
                "timestamp" to System.currentTimeMillis()
            )
            locationsCollection.document(userId).set(locationData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUserLocations(): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = locationsCollection.get().await()
            val locations = snapshot.documents.map { doc ->
                doc.data ?: emptyMap()
            }
            Result.success(locations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}