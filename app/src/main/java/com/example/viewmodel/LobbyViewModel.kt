package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ViperRepository
import com.example.model.ArenaRoom
import com.example.model.BotDifficulty
import com.example.model.DailyClaimResult
import com.example.model.DailyRewardCatalog
import com.example.model.DailyRewardStatus
import com.example.model.LeaderboardEntry
import com.example.model.SkinCatalog
import com.example.model.SnakePattern
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class LeaderboardViewData(
    val topTen: List<LeaderboardEntry>,
    val allEntriesCount: Int,
    val myRank: Int?,
    val myEntry: LeaderboardEntry?
)

class LobbyViewModel(
    private val repository: ViperRepository,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String?,
    private val emailVerified: Boolean
) : ViewModel() {

    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _lastDailyClaimResult = MutableStateFlow<DailyClaimResult?>(null)
    val lastDailyClaimResult: StateFlow<DailyClaimResult?> = _lastDailyClaimResult.asStateFlow()

    private val _shouldAutoShowDailyDialog = MutableStateFlow(false)
    val shouldAutoShowDailyDialog: StateFlow<Boolean> = _shouldAutoShowDailyDialog.asStateFlow()
    private var hasAutoPromptedDaily = false

    val userProfileState: StateFlow<UiState<UserProfile>> =
        repository.observeOrCreateUserProfile(currentUserId, userDisplayName)
            .onEach { profile ->
                if (!hasAutoPromptedDaily) {
                    hasAutoPromptedDaily = true
                    val status = DailyRewardCatalog.computeStatus(
                        currentStreak = profile.dailyStreak.toInt(),
                        lastClaimEpochMs = profile.lastDailyClaimAt?.toDate()?.time
                    )
                    if (status.canClaimNow) {
                        _shouldAutoShowDailyDialog.value = true
                    }
                }
            }
            .map<UserProfile, UiState<UserProfile>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Profile observation failed", root)
                emit(UiState.Error(root.message ?: "Failed to load profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    private val clockTicker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    val dailyRewardStatus: StateFlow<DailyRewardStatus> =
        combine(userProfileState, clockTicker) { state, nowMs ->
            val profile = (state as? UiState.Success)?.data
            DailyRewardCatalog.computeStatus(
                currentStreak = (profile?.dailyStreak ?: 0L).toInt(),
                lastClaimEpochMs = profile?.lastDailyClaimAt?.toDate()?.time,
                nowEpochMs = nowMs
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = DailyRewardCatalog.computeStatus(0, null)
        )

    val leaderboardState: StateFlow<UiState<LeaderboardViewData>> =
        repository.observeLeaderboard(currentUserId)
            .map<List<LeaderboardEntry>, UiState<LeaderboardViewData>> { sorted ->
                val myIdx = sorted.indexOfFirst { it.userId == currentUserId }
                val myRank = if (myIdx >= 0) myIdx + 1 else null
                val myEntry = if (myIdx >= 0) sorted[myIdx] else null
                UiState.Success(
                    LeaderboardViewData(
                        topTen = sorted.take(10),
                        allEntriesCount = sorted.size,
                        myRank = myRank,
                        myEntry = myEntry
                    )
                )
            }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Leaderboard observation failed", root)
                emit(UiState.Error(root.message ?: "Failed to load leaderboard"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val publicRoomsState: StateFlow<UiState<List<ArenaRoom>>> =
        repository.observePublicRooms(currentUserId)
            .map<List<ArenaRoom>, UiState<List<ArenaRoom>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = unwrapFirestoreError(error)
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Public rooms observation failed", root)
                emit(UiState.Error(root.message ?: "Failed to load multiplayer rooms"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    init {
        viewModelScope.launch {
            val result = repository.resolveIsOwner(currentUserId, userEmail, emailVerified)
            _isOwner.value = result.getOrDefault(false)
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun dismissAutoDailyDialog() {
        _shouldAutoShowDailyDialog.value = false
    }

    fun clearDailyClaimResult() {
        _lastDailyClaimResult.value = null
    }

    fun claimDailyReward() {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.claimDailyReward(currentUserId)
            _isBusy.value = false
            res.onSuccess { claimed ->
                _lastDailyClaimResult.value = claimed
                val skinMsg = claimed.unlockedSkinId?.let { id ->
                    " + Unlocked ${SkinCatalog.getById(id).name}!"
                }.orEmpty()
                _actionMessage.value = "Day ${claimed.claimedDay} claimed: +${claimed.coinsAwarded} Venom Coins$skinMsg"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Daily reward is not ready yet"
            }
        }
    }

    fun unlockShopSkin(skinId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.unlockShopSkinWithCoins(currentUserId, skinId)
            _isBusy.value = false
            val skinName = SkinCatalog.getById(skinId).name
            _actionMessage.value = if (res.isSuccess) {
                "Unlocked & equipped $skinName!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Could not unlock $skinName"
            }
        }
    }

    fun saveCustomization(
        displayName: String,
        skinId: String,
        pattern: SnakePattern,
        preferredDifficulty: BotDifficulty
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateCustomization(
                uid = currentUserId,
                displayName = displayName,
                skinId = skinId,
                pattern = pattern,
                preferredDifficulty = preferredDifficulty,
                isOwner = _isOwner.value
            )
            _isBusy.value = false
            _actionMessage.value = if (res.isSuccess) {
                "Snake customization saved!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to save customization"
            }
        }
    }

    fun updatePreferredDifficulty(difficulty: BotDifficulty) {
        viewModelScope.launch {
            repository.updatePreferredDifficulty(currentUserId, difficulty)
        }
    }

    fun createMultiplayerRoom(
        name: String,
        botDifficulty: BotDifficulty,
        botCount: Int,
        maxPlayers: Int,
        onRoomReady: (ArenaRoom) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createRoom(
                hostId = currentUserId,
                roomName = name,
                botDifficulty = botDifficulty,
                botCount = botCount,
                maxPlayers = maxPlayers
            )
            _isBusy.value = false
            res.onSuccess { room ->
                onRoomReady(room)
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not create room"
            }
        }
    }

    fun joinMultiplayerRoom(roomId: String, onRoomReady: (ArenaRoom) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.joinRoom(roomId, currentUserId)
            _isBusy.value = false
            res.onSuccess { room ->
                onRoomReady(room)
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Could not join room"
            }
        }
    }

    fun joinRoomByCode(code: String, onRoomReady: (ArenaRoom) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.joinRoomByCode(code, currentUserId)
            _isBusy.value = false
            res.onSuccess { room ->
                onRoomReady(room)
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "Room code not found"
            }
        }
    }

    // --- OWNER-ONLY CLOUD ACTIONS ---

    fun ownerSetMyLeaderboardLength(
        displayName: String,
        skinId: String,
        pattern: SnakePattern,
        newLength: Long,
        newKills: Long
    ) {
        if (!_isOwner.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.ownerSetLeaderboardLength(
                targetUid = currentUserId,
                displayName = displayName,
                skinId = skinId,
                pattern = pattern,
                newMaxLength = newLength,
                newKills = newKills
            )
            _isBusy.value = false
            _actionMessage.value = if (res.isSuccess) {
                "Owner Override: Global rank length updated to $newLength!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Override failed"
            }
        }
    }

    fun ownerDeleteLeaderboardEntry(targetUid: String) {
        if (!_isOwner.value) return
        viewModelScope.launch {
            val res = repository.ownerDeleteLeaderboardEntry(targetUid)
            _actionMessage.value = if (res.isSuccess) {
                "Entry removed from Global Leaderboard."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Delete failed"
            }
        }
    }

    fun ownerCloseRoom(roomId: String) {
        if (!_isOwner.value) return
        viewModelScope.launch {
            val res = repository.ownerCloseRoom(roomId)
            _actionMessage.value = if (res.isSuccess) {
                "Multiplayer room closed."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Close room failed"
            }
        }
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private companion object {
        const val TAG = "LobbyViewModel"
    }
}
