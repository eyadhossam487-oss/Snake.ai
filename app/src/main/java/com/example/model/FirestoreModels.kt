package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Viper",
    val selectedSkinId: String = "cyber_viper",
    val selectedPattern: String = "SOLID",
    val preferredDifficulty: String = "MEDIUM",
    val maxLength: Long = 25L,
    val totalKills: Long = 0L,
    val gamesPlayed: Long = 0L,
    val coins: Long = 0L,
    val dailyStreak: Long = 0L,
    val lastDailyClaimAt: Timestamp? = null,
    val unlockedSkinIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Viper",
    val skinId: String = "cyber_viper",
    val pattern: String = "SOLID",
    val maxLength: Long = 25L,
    val totalKills: Long = 0L,
    val visibility: String = "PUBLIC",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ArenaRoom(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val name: String = "Cyber Grid",
    val roomCode: String = "VIPER1",
    val status: String = "WAITING",
    val visibility: String = "PUBLIC",
    val botDifficulty: String = "MEDIUM",
    val botCount: Long = 6L,
    val maxPlayers: Long = 8L,
    val playerCount: Long = 1L,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RemoteSnakeDoc(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val roomVisibility: String = "PUBLIC",
    val displayName: String = "Viper",
    val skinId: String = "cyber_viper",
    val pattern: String = "SOLID",
    val botControlled: Boolean = false,
    val alive: Boolean = true,
    val boosting: Boolean = false,
    val godMode: Boolean = false,
    val x: Double = 1300.0,
    val y: Double = 1300.0,
    val angle: Double = 0.0,
    val score: Long = 25L,
    val kills: Long = 0L,
    val points: List<Double> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppOwnerConfig(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
