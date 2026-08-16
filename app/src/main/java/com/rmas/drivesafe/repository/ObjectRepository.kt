package com.rmas.drivesafe.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rmas.drivesafe.model.MapObject
import com.rmas.drivesafe.model.Rating
import kotlinx.coroutines.tasks.await

class ObjectRepository {
    private val db = FirebaseFirestore.getInstance()
    private val objectsCollection = db.collection("objects")
    private val ratingsCollection = db.collection("ratings")

    suspend fun addObject(mapObject: MapObject): Result<String> {
        return try {
            val docRef = objectsCollection.add(mapObject).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getObject(objectId: String): Result<MapObject> {
        return try {
            val document = objectsCollection.document(objectId).get().await()
            val obj = document.toObject(MapObject::class.java)?.copy(id = document.id)
            if (obj != null) {
                Result.success(obj)
            } else {
                Result.failure(Exception("Objekat nije pronadjen"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllObjects(): Result<List<MapObject>> {
        return try {
            val snapshot = objectsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val objects = snapshot.documents.map { doc ->
                doc.toObject(MapObject::class.java)!!.copy(id = doc.id)
            }
            Result.success(objects)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getObjectsByAuthor(authorId: String): Result<List<MapObject>> {
        return try {
            val snapshot = objectsCollection
                .whereEqualTo("authorId", authorId)
                .get()
                .await()
            val objects = snapshot.documents.map { doc ->
                doc.toObject(MapObject::class.java)!!.copy(id = doc.id)
            }
            Result.success(objects)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addRating(rating: Rating): Result<Unit> {
        return try {
            ratingsCollection.add(rating).await()
            val objDoc = objectsCollection.document(rating.objectId).get().await()
            val obj = objDoc.toObject(MapObject::class.java)
            if (obj != null) {
                val newCount = obj.ratingCount + 1
                val newRating = (obj.rating * obj.ratingCount + rating.rating) / newCount
                objectsCollection.document(rating.objectId).update(
                    mapOf(
                        "rating" to newRating,
                        "ratingCount" to newCount
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteObject(objectId: String): Result<Unit> {
        return try {
            objectsCollection.document(objectId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}