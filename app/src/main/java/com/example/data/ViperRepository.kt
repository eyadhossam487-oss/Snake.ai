package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AppOwnerConfig
import com.example.model.ArenaRoom
import com.example.model.BotDifficulty
import com.example.model.DailyClaimResult
import com.example.model.DailyRewardCatalog
import com.example.model.LeaderboardEntry
import com.example.model.RemoteSnakeDoc
import com.example.model.SkinCatalog
import com.example.model.SnakePattern
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ViperRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val BOOTSTRAPPED_OWNER_EMAIL = "eyadhossam487@gmail.com"
        private val VALID_SKIN_IDS = setOf(
            "cyber_viper",
            "plasma_cobra",
            "solar_mamba",
            "venom_jade",
            "frost_wyrm",
            "magma_python",
            "prismatic_hydra",
            "void_phantom",
            "quantum_anaconda",
            "chrono_basilisk",
            "nebula_leviathan",
            "solar_eclipse",
            "apex_sovereign"
        )
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun sanitizeName(raw: String): String {
        val trimmed = raw.trim().replace(Regex("\\s+"), " ")
        return trimmed.ifBlank { "Viper" }.take(24)
    }

    private fun sanitizeRoomName(raw: String): String {
        val trimmed = raw.trim().replace(Regex("\\s+"), " ")
        return trimmed.ifBlank { "Neon Arena" }.take(32).let {
            if (it.length < 2) "$it Arena" else it
        }
    }

    /**
     * Claims /app_config/owner if no account has ever signed in yet, locking ownership
     * to the very first account created (plus verified bootstrapped owner email fallback).
     */
    suspend fun resolveIsOwner(uid: String, email: String?, emailVerified: Boolean): Result<Boolean> {
        val docPath = "app_config/owner"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Signed-in user mismatch." }

            val ownerRef = db.collection("app_config").document("owner")
            val existing = ownerRef.get().await()
            val firstAccountUid = if (existing.exists()) {
                existing.toObject(AppOwnerConfig::class.java)?.ownerUid.orEmpty()
            } else {
                try {
                    ownerRef.set(
                        mapOf(
                            "ownerUid" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    uid
                } catch (raceError: Exception) {
                    // If another user claimed it first simultaneously, re-read
                    ownerRef.get().await().toObject(AppOwnerConfig::class.java)?.ownerUid.orEmpty()
                }
            }

            val isFirstAccount = firstAccountUid == uid
            val isVerifiedBootstrapEmail =
                email != null &&
                    email.equals(BOOTSTRAPPED_OWNER_EMAIL, ignoreCase = true) &&
                    emailVerified
            Result.success(isFirstAccount || isVerifiedBootstrapEmail)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            val fallbackEmailMatch =
                email != null &&
                    email.equals(BOOTSTRAPPED_OWNER_EMAIL, ignoreCase = true) &&
                    emailVerified
            Result.success(fallbackEmailMatch)
        }
    }

    fun observeOrCreateUserProfile(uid: String, defaultDisplayName: String): Flow<UserProfile> = flow {
        val docRef = db.collection("users").document(uid)
        val path = docRef.path
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) {
            val cleanName = sanitizeName(defaultDisplayName)
            val payload = mapOf(
                "userId" to uid,
                "displayName" to cleanName,
                "selectedSkinId" to "cyber_viper",
                "selectedPattern" to SnakePattern.SOLID.name,
                "preferredDifficulty" to BotDifficulty.MEDIUM.name,
                "maxLength" to 25L,
                "totalKills" to 0L,
                "gamesPlayed" to 0L,
                "coins" to 0L,
                "dailyStreak" to 0L,
                "unlockedSkinIds" to emptyList<String>(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        }
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: UserProfile(id = uid, userId = uid, displayName = sanitizeName(defaultDisplayName))
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun updateCustomization(
        uid: String,
        displayName: String,
        skinId: String,
        pattern: SnakePattern,
        preferredDifficulty: BotDifficulty,
        isOwner: Boolean = false
    ): Result<Unit> {
        val docPath = "users/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Signed-in user changed." }
            val cleanName = sanitizeName(displayName)
            val requestedSkin = if (skinId in VALID_SKIN_IDS) skinId else "cyber_viper"

            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            val prevProfile = userSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val unlocked = prevProfile?.unlockedSkinIds ?: emptyList()
            val skinObj = SkinCatalog.getById(requestedSkin)
            if (!SkinCatalog.isSkinUnlocked(skinObj, unlocked, isOwner)) {
                throw IllegalStateException("${skinObj.name} is still locked.")
            }

            userRef.update(
                mapOf(
                    "displayName" to cleanName,
                    "selectedSkinId" to requestedSkin,
                    "selectedPattern" to pattern.name,
                    "preferredDifficulty" to preferredDifficulty.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Also sync skin/name to leaderboard entry if it already exists
            val lbRef = db.collection("leaderboard").document(uid)
            val lbSnap = lbRef.get().await()
            if (lbSnap.exists()) {
                lbRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "skinId" to requestedSkin,
                        "pattern" to pattern.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun claimDailyReward(uid: String): Result<DailyClaimResult> {
        val docPath = "users/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Signed-in user changed." }

            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            val profile = userSnap.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw IllegalStateException("Player profile not initialized yet.")

            val status = DailyRewardCatalog.computeStatus(
                currentStreak = profile.dailyStreak.toInt(),
                lastClaimEpochMs = profile.lastDailyClaimAt?.toDate()?.time
            )
            if (!status.canClaimNow) {
                throw IllegalStateException(
                    "Next daily reward ready in ${DailyRewardCatalog.formatCountdown(status.remainingCooldownMs)}."
                )
            }

            val tier = DailyRewardCatalog.tierForDay(status.nextRewardDay)
            val prevUnlocked = profile.unlockedSkinIds
            val newSkinUnlocked = tier.skinId?.takeIf { it !in prevUnlocked }
            val duplicateBonus = if (tier.skinId != null && newSkinUnlocked == null) 250 else 0
            val coinsAwarded = (tier.coinsAmount + duplicateBonus).coerceIn(0, 1000)
            val newBalance = (profile.coins + coinsAwarded.toLong()).coerceAtMost(10_000_000L)
            val updatedUnlocked = if (newSkinUnlocked != null) {
                (prevUnlocked + newSkinUnlocked).distinct().take(32)
            } else {
                prevUnlocked
            }

            val updatePayload = mutableMapOf<String, Any>(
                "coins" to newBalance,
                "dailyStreak" to status.nextRewardDay.toLong(),
                "lastDailyClaimAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (newSkinUnlocked != null || prevUnlocked.isNotEmpty()) {
                updatePayload["unlockedSkinIds"] = updatedUnlocked
            }

            userRef.update(updatePayload).await()

            Result.success(
                DailyClaimResult(
                    claimedDay = status.nextRewardDay,
                    coinsAwarded = coinsAwarded,
                    unlockedSkinId = newSkinUnlocked,
                    newBalance = newBalance,
                    newStreak = status.nextRewardDay
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun unlockShopSkinWithCoins(uid: String, skinId: String): Result<Unit> {
        val docPath = "users/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid) { "Signed-in user changed." }
            val skin = SkinCatalog.getById(skinId)
            require(skin.id == skinId && skin.unlockCostCoins > 0) {
                "This skin cannot be purchased with Venom Coins."
            }

            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            val profile = userSnap.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw IllegalStateException("Player profile not found.")

            if (skinId in profile.unlockedSkinIds) {
                return Result.success(Unit)
            }
            if (profile.coins < skin.unlockCostCoins.toLong()) {
                throw IllegalStateException(
                    "Need ${skin.unlockCostCoins - profile.coins} more Venom Coins to unlock ${skin.name}."
                )
            }

            val newBalance = profile.coins - skin.unlockCostCoins.toLong()
            val newUnlocked = (profile.unlockedSkinIds + skinId).distinct().take(32)

            userRef.update(
                mapOf(
                    "coins" to newBalance,
                    "unlockedSkinIds" to newUnlocked,
                    "selectedSkinId" to skinId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val lbRef = db.collection("leaderboard").document(uid)
            if (lbRef.get().await().exists()) {
                lbRef.update(
                    mapOf(
                        "skinId" to skinId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updatePreferredDifficulty(uid: String, difficulty: BotDifficulty): Result<Unit> {
        val docPath = "users/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid)
            db.collection("users").document(uid).update(
                mapOf(
                    "preferredDifficulty" to difficulty.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordMatchResult(
        uid: String,
        displayName: String,
        skinId: String,
        pattern: SnakePattern,
        lengthAchieved: Int,
        killsEarned: Int
    ): Result<Boolean> {
        val userPath = "users/$uid"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid)
            val cleanName = sanitizeName(displayName)
            val safeSkin = if (skinId in VALID_SKIN_IDS) skinId else "cyber_viper"
            val boundedLength = lengthAchieved.coerceIn(10, 10_000_000).toLong()
            val boundedKills = killsEarned.coerceIn(0, 10_000_000).toLong()

            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            val prevProfile = userSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            val prevMax = prevProfile?.maxLength ?: 25L
            val newMax = maxOf(prevMax, boundedLength)
            val isNewRecord = boundedLength > prevMax
            val newTotalKills = ((prevProfile?.totalKills ?: 0L) + boundedKills).coerceAtMost(10_000_000L)
            val newGamesPlayed = ((prevProfile?.gamesPlayed ?: 0L) + 1L).coerceAtMost(10_000_000L)

            if (userSnap.exists()) {
                userRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "selectedSkinId" to safeSkin,
                        "selectedPattern" to pattern.name,
                        "maxLength" to newMax,
                        "totalKills" to newTotalKills,
                        "gamesPlayed" to newGamesPlayed,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Upsert global leaderboard entry for this player
            val lbRef = db.collection("leaderboard").document(uid)
            val lbSnap = lbRef.get().await()
            if (lbSnap.exists()) {
                val prevLbMax = (lbSnap.getLong("maxLength") ?: 25L)
                lbRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "skinId" to safeSkin,
                        "pattern" to pattern.name,
                        "maxLength" to maxOf(prevLbMax, newMax),
                        "totalKills" to newTotalKills,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                lbRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "skinId" to safeSkin,
                        "pattern" to pattern.name,
                        "maxLength" to newMax,
                        "totalKills" to newTotalKills,
                        "visibility" to "PUBLIC",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(isNewRecord)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userPath)
            Result.failure(e)
        }
    }

    fun observeLeaderboard(uid: String): Flow<List<LeaderboardEntry>> = flow {
        val path = "leaderboard"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "PUBLIC")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LeaderboardEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<LeaderboardEntry> { it.maxLength }
                            .thenByDescending { it.totalKills }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePublicRooms(uid: String): Flow<List<ArenaRoom>> = flow {
        val path = "rooms"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "PUBLIC")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ArenaRoom::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                        .filter { it.status != "FINISHED" }
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createRoom(
        hostId: String,
        roomName: String,
        botDifficulty: BotDifficulty,
        botCount: Int,
        maxPlayers: Int
    ): Result<ArenaRoom> {
        val roomId = "room_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docPath = "rooms/$roomId"
        return try {
            val uid = requireUserId()
            check(uid == hostId)
            val cleanName = sanitizeRoomName(roomName)
            val code = buildString {
                val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
                repeat(6) { append(chars.random()) }
            }
            val safeBots = botCount.coerceIn(0, 12).toLong()
            val safeMax = maxPlayers.coerceIn(2, 12).toLong()

            val payload = mapOf(
                "hostId" to uid,
                "name" to cleanName,
                "roomCode" to code,
                "status" to "WAITING",
                "visibility" to "PUBLIC",
                "botDifficulty" to botDifficulty.name,
                "botCount" to safeBots,
                "maxPlayers" to safeMax,
                "playerCount" to 1L,
                "memberUids" to listOf(uid),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("rooms").document(roomId).set(payload).await()
            // Immediately transition to ACTIVE so players can jump right in
            db.collection("rooms").document(roomId).update(
                mapOf(
                    "status" to "ACTIVE",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(
                ArenaRoom(
                    id = roomId,
                    hostId = uid,
                    name = cleanName,
                    roomCode = code,
                    status = "ACTIVE",
                    visibility = "PUBLIC",
                    botDifficulty = botDifficulty.name,
                    botCount = safeBots,
                    maxPlayers = safeMax,
                    playerCount = 1L,
                    memberUids = listOf(uid)
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinRoom(roomId: String, uid: String): Result<ArenaRoom> {
        val docPath = "rooms/$roomId"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid)
            val roomRef = db.collection("rooms").document(roomId)
            val snap = roomRef.get().await()
            val room = snap.toObject(ArenaRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: throw IllegalStateException("Room not found")

            if (room.status == "FINISHED") {
                throw IllegalStateException("This arena match has already ended.")
            }
            if (uid !in room.memberUids) {
                if (room.playerCount >= room.maxPlayers) {
                    throw IllegalStateException("This arena room is full.")
                }
                val updatedMembers = (room.memberUids + uid).distinct()
                roomRef.update(
                    mapOf(
                        "memberUids" to updatedMembers,
                        "playerCount" to updatedMembers.size.toLong(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                return Result.success(
                    room.copy(
                        memberUids = updatedMembers,
                        playerCount = updatedMembers.size.toLong()
                    )
                )
            }
            Result.success(room)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun joinRoomByCode(code: String, uid: String): Result<ArenaRoom> {
        val normalized = code.trim().uppercase()
        return try {
            requireUserId()
            require(normalized.length in 4..8) { "Room code must be 4-8 characters." }
            val snap = db.collection("rooms")
                .whereEqualTo("visibility", "PUBLIC")
                .get()
                .await()
            val matchingRoom = snap.toObjects(
                ArenaRoom::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).firstOrNull { it.roomCode.equals(normalized, ignoreCase = true) && it.status != "FINISHED" }
                ?: throw IllegalStateException("No active arena found with code $normalized.")
            joinRoom(matchingRoom.id, uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "rooms")
            Result.failure(e)
        }
    }

    suspend fun leaveRoom(roomId: String, uid: String): Result<Unit> {
        val docPath = "rooms/$roomId"
        return try {
            val currentUid = requireUserId()
            check(currentUid == uid)
            // Remove own snake first
            runCatching {
                db.collection("rooms").document(roomId)
                    .collection("snakes").document(uid)
                    .delete().await()
            }

            val roomRef = db.collection("rooms").document(roomId)
            val snap = roomRef.get().await()
            val room = snap.toObject(ArenaRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return Result.success(Unit)

            if (room.hostId == uid && room.status != "FINISHED") {
                roomRef.update(
                    mapOf(
                        "status" to "FINISHED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else if (uid in room.memberUids && room.status != "FINISHED") {
                val updatedMembers = room.memberUids.filterNot { it == uid }
                roomRef.update(
                    mapOf(
                        "memberUids" to updatedMembers,
                        "playerCount" to updatedMembers.size.toLong(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeRoomSnakes(roomId: String, uid: String): Flow<List<RemoteSnakeDoc>> = flow {
        val path = "rooms/$roomId/snakes"
        emitAll(
            db.collection("rooms").document(roomId).collection("snakes")
                .whereEqualTo("roomVisibility", "PUBLIC")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RemoteSnakeDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertRoomSnake(
        roomId: String,
        snakeId: String,
        ownerId: String,
        displayName: String,
        skinId: String,
        pattern: SnakePattern,
        botControlled: Boolean,
        alive: Boolean,
        boosting: Boolean,
        godMode: Boolean,
        x: Double,
        y: Double,
        angle: Double,
        score: Long,
        kills: Long,
        points: List<Double>,
        createdBefore: Boolean
    ): Result<Boolean> {
        val docPath = "rooms/$roomId/snakes/$snakeId"
        return try {
            val uid = requireUserId()
            check(uid == ownerId)
            val safePoints = points.take(160)
            val safeSkin = if (skinId in VALID_SKIN_IDS) skinId else "cyber_viper"
            val docRef = db.collection("rooms").document(roomId)
                .collection("snakes").document(snakeId)

            if (createdBefore) {
                docRef.update(
                    mapOf(
                        "displayName" to sanitizeName(displayName),
                        "skinId" to safeSkin,
                        "pattern" to pattern.name,
                        "alive" to alive,
                        "boosting" to boosting,
                        "godMode" to godMode,
                        "x" to x.coerceIn(0.0, 2600.0),
                        "y" to y.coerceIn(0.0, 2600.0),
                        "angle" to angle,
                        "score" to score.coerceIn(0L, 10_000_000L),
                        "kills" to kills.coerceIn(0L, 10_000_000L),
                        "points" to safePoints,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(true)
            } else {
                val snap = docRef.get().await()
                if (snap.exists()) {
                    docRef.update(
                        mapOf(
                            "displayName" to sanitizeName(displayName),
                            "skinId" to safeSkin,
                            "pattern" to pattern.name,
                            "alive" to alive,
                            "boosting" to boosting,
                            "godMode" to godMode,
                            "x" to x.coerceIn(0.0, 2600.0),
                            "y" to y.coerceIn(0.0, 2600.0),
                            "angle" to angle,
                            "score" to score.coerceIn(0L, 10_000_000L),
                            "kills" to kills.coerceIn(0L, 10_000_000L),
                            "points" to safePoints,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    docRef.set(
                        mapOf(
                            "ownerId" to uid,
                            "roomVisibility" to "PUBLIC",
                            "displayName" to sanitizeName(displayName),
                            "skinId" to safeSkin,
                            "pattern" to pattern.name,
                            "botControlled" to botControlled,
                            "alive" to alive,
                            "boosting" to boosting,
                            "godMode" to godMode,
                            "x" to x.coerceIn(0.0, 2600.0),
                            "y" to y.coerceIn(0.0, 2600.0),
                            "angle" to angle,
                            "score" to score.coerceIn(0L, 10_000_000L),
                            "kills" to kills.coerceIn(0L, 10_000_000L),
                            "points" to safePoints,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- OWNER-ONLY CLOUD ADMIN OPERATIONS ---

    suspend fun ownerSetLeaderboardLength(
        targetUid: String,
        displayName: String,
        skinId: String,
        pattern: SnakePattern,
        newMaxLength: Long,
        newKills: Long
    ): Result<Unit> {
        val docPath = "leaderboard/$targetUid"
        return try {
            requireUserId()
            val boundedLen = newMaxLength.coerceIn(10L, 10_000_000L)
            val boundedKills = newKills.coerceIn(0L, 10_000_000L)
            val lbRef = db.collection("leaderboard").document(targetUid)
            val snap = lbRef.get().await()
            if (snap.exists()) {
                lbRef.update(
                    mapOf(
                        "maxLength" to boundedLen,
                        "totalKills" to boundedKills,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                lbRef.set(
                    mapOf(
                        "userId" to targetUid,
                        "displayName" to sanitizeName(displayName),
                        "skinId" to (if (skinId in VALID_SKIN_IDS) skinId else "apex_sovereign"),
                        "pattern" to pattern.name,
                        "maxLength" to boundedLen,
                        "totalKills" to boundedKills,
                        "visibility" to "PUBLIC",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Also sync target user's profile maxLength if it exists
            val userRef = db.collection("users").document(targetUid)
            if (userRef.get().await().exists()) {
                userRef.update(
                    mapOf(
                        "maxLength" to boundedLen,
                        "totalKills" to boundedKills,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun ownerDeleteLeaderboardEntry(targetUid: String): Result<Unit> {
        val docPath = "leaderboard/$targetUid"
        return try {
            requireUserId()
            db.collection("leaderboard").document(targetUid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun ownerCloseRoom(roomId: String): Result<Unit> {
        val docPath = "rooms/$roomId"
        return try {
            requireUserId()
            db.collection("rooms").document(roomId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
