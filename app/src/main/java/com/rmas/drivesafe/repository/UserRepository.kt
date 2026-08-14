package com.rmas.drivesafe.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rmas.drivesafe.model.User
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun createUser(user: User): Result<Unit> {
        return try {
            usersCollection.document(user.id).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): Result<User> {
        return try {
            val document = usersCollection.document(userId).get().await()
            val user = document.toObject(User::class.java)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Korisnik nije pronadjen"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserPoints(userId: String, pointsToAdd: Int): Result<Unit> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val currentPoints = userDoc.getLong("points")?.toInt() ?: 0
            val newPoints = currentPoints + pointsToAdd
            usersCollection.document(userId).update("points", newPoints).await()
            updateUserRank(userId, newPoints)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRank(userId: String, points: Int): Result<Unit> {
        return try {
            val rank = when {
                points >= 200 -> "Expert"
                points >= 100 -> "Napredni"
                points >= 50 -> "Srednji"
                points >= 20 -> "Pocetnik+"
                else -> "Pocetnik"
            }
            usersCollection.document(userId).update("rank", rank).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = usersCollection
                .orderBy("points", Query.Direction.DESCENDING)
                .get()
                .await()
            val users = snapshot.toObjects(User::class.java)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}