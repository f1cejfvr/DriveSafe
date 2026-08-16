package com.rmas.drivesafe.model

import java.util.Date
data class User(
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val phone: String = "",
    val profileImageUrl: String = "",
    val points: Int = 0,
    val rank: String = "Pocetnik"
)

data class MapObject(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val authorId: String = "",
    val authorName: String = "",
    val createdAt: Date? = null,
    val imageUrl: String = "",
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val serviceType: String = "",
    val phone: String = "",
    val workingHours: String = "",
    val dangerType: String = "",
    val dangerLevel: Int = 0,
    val parkingType: String = "",
    val totalSpots: Int = 0,
    val pricePerHour: Double = 0.0
)

data class Rating(
    val id: String = "",
    val objectId: String = "",
    val userId: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val createdAt: Date? = null
)