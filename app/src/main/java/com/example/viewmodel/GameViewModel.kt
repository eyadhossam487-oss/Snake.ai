package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ViperRepository
import com.example.game.ArenaEngine
import com.example.game.ArenaSnapshot
import com.example.model.ActiveSnake
import com.example.model.ArenaRoom
import com.example.model.BotDifficulty
import com.example.model.GameSettings
import com.example.model.MatchSummary
import com.example.model.OwnerCheatState
import com.example.model.SnakePattern
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ArenaSessionConfig(
    val room: ArenaRoom?,
    val botDifficulty: BotDifficulty,
    val botCount: Int,
    val playerName: String,
    val skinId: String,
    val pattern: SnakePattern
)

class GameViewModel(
    private val repository: ViperRepository,
    private val currentUserId: String,
    private val isOwner: Boolean
) : ViewModel() {

    private var engine: ArenaEngine? = null
    private var gameLoopJob: Job? = null
    private var roomSyncJob: Job? = null
    private var roomObserveJob: Job? = null
    private var activeRoom: ArenaRoom? = null
    private var createdSnakeDocs = mutableSetOf<String>()
    private var sessionRecorded = false

    private val _activeSession = MutableStateFlow<ArenaSessionConfig?>(null)
    val activeSession: StateFlow<ArenaSessionConfig?> = _activeSession.asStateFlow()

    private val _gameSettings = MutableStateFlow(GameSettings())
    val gameSettings: StateFlow<GameSettings> = _gameSettings.asStateFlow()

    private val _snapshot = MutableStateFlow<ArenaSnapshot?>(null)
    val snapshot: StateFlow<ArenaSnapshot?> = _snapshot.asStateFlow()

    private val _cameraPos = MutableStateFlow(1300f to 1300f)
    val cameraPos: StateFlow<Pair<Float, Float>> = _cameraPos.asStateFlow()

    private val _matchSummary = MutableStateFlow<MatchSummary?>(null)
    val matchSummary: StateFlow<MatchSummary?> = _matchSummary.asStateFlow()

    private val _ownerCheats = MutableStateFlow(OwnerCheatState())
    val ownerCheats: StateFlow<OwnerCheatState> = _ownerCheats.asStateFlow()

    private val _activeDifficulty = MutableStateFlow(BotDifficulty.MEDIUM)
    val activeDifficulty: StateFlow<BotDifficulty> = _activeDifficulty.asStateFlow()

    private val _activeBotCount = MutableStateFlow(6)
    val activeBotCount: StateFlow<Int> = _activeBotCount.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private var steeringAngle: Float? = null
    private var wantsBoost: Boolean = false
    private var currentConfig: ArenaSessionConfig? = null

    fun startArena(config: ArenaSessionConfig) {
        if (_activeSession.value != null) {
            leaveArena()
        }
        currentConfig = config
        _activeSession.value = config
        activeRoom = config.room
        sessionRecorded = false
        createdSnakeDocs.clear()
        _matchSummary.value = null
        _isPaused.value = false
        _activeDifficulty.value = config.botDifficulty
        _activeBotCount.value = config.botCount

        val isHostOrSolo = config.room == null || config.room.hostId == currentUserId
        val newEngine = ArenaEngine(
            playerUid = currentUserId,
            playerName = config.playerName,
            playerSkinId = config.skinId,
            playerPattern = config.pattern,
            initialBotCount = config.botCount,
            initialDifficulty = config.botDifficulty,
            isHostOrSolo = isHostOrSolo
        )
        engine = newEngine

        // Initial snapshot
        val firstSnap = newEngine.step(0.016f, null, false, currentSafeCheats())
        _snapshot.value = firstSnap
        firstSnap.playerSnake?.let { _cameraPos.value = it.x to it.y }

        // Observe & sync multiplayer snakes if in a room
        val room = config.room
        if (room != null) {
            startMultiplayerSync(room)
        }

        startGameLoop()
    }

    fun respawn() {
        val cfg = currentConfig ?: return
        val eng = engine ?: return
        sessionRecorded = false
        _matchSummary.value = null
        _isPaused.value = false
        steeringAngle = null
        wantsBoost = false
        eng.resetMatch(cfg.skinId, cfg.pattern, cfg.playerName)
    }

    fun setSteeringAngle(angle: Float?) {
        steeringAngle = angle
    }

    fun setBoosting(boosting: Boolean) {
        wantsBoost = boosting
    }

    fun togglePause(paused: Boolean) {
        if (paused) {
            steeringAngle = null
            wantsBoost = false
        }
        _isPaused.value = paused
    }

    fun pauseAndKeepSession() {
        steeringAngle = null
        wantsBoost = false
        _isPaused.value = true
    }

    fun resumeExistingSession() {
        _isPaused.value = false
    }

    fun updateSettings(transform: (GameSettings) -> GameSettings) {
        _gameSettings.value = transform(_gameSettings.value)
    }

    fun setBotDifficulty(difficulty: BotDifficulty) {
        _activeDifficulty.value = difficulty
        engine?.updateDifficulty(difficulty)
    }

    fun setBotCount(count: Int) {
        _activeBotCount.value = count.coerceIn(0, 12)
        engine?.updateBotCount(count)
    }

    // --- OWNER-ONLY CHEAT CONTROLS ---
    fun toggleGodMode() {
        if (!isOwner) return
        _ownerCheats.value = _ownerCheats.value.copy(godMode = !_ownerCheats.value.godMode)
    }

    fun togglePermaSpeed() {
        if (!isOwner) return
        _ownerCheats.value = _ownerCheats.value.copy(
            permaSpeedOverdrive = !_ownerCheats.value.permaSpeedOverdrive
        )
    }

    fun togglePermaMagnet() {
        if (!isOwner) return
        _ownerCheats.value = _ownerCheats.value.copy(
            permaMegaMagnet = !_ownerCheats.value.permaMegaMagnet
        )
    }

    fun toggleFreezeBots() {
        if (!isOwner) return
        _ownerCheats.value = _ownerCheats.value.copy(freezeBots = !_ownerCheats.value.freezeBots)
    }

    fun addOwnerLength(amount: Int = 500) {
        if (!isOwner) return
        engine?.applyOwnerLengthBoost(amount)
    }

    fun spawnFoodRain() {
        if (!isOwner) return
        engine?.spawnOwnerFoodRain()
    }

    fun eliminateAllBots() {
        if (!isOwner) return
        engine?.eliminateAllBots()
    }

    fun setOwnerBotDifficultyOverride(difficulty: BotDifficulty?) {
        if (!isOwner) return
        _ownerCheats.value = _ownerCheats.value.copy(botDifficultyOverride = difficulty)
        if (difficulty != null) {
            _activeDifficulty.value = difficulty
            engine?.updateDifficulty(difficulty)
        }
    }

    private fun currentSafeCheats(): OwnerCheatState =
        if (isOwner) _ownerCheats.value else OwnerCheatState()

    private fun startGameLoop() {
        gameLoopJob = viewModelScope.launch {
            var lastTimeNs = System.nanoTime()
            while (isActive) {
                delay(16L)
                val nowNs = System.nanoTime()
                if (_isPaused.value) {
                    lastTimeNs = nowNs
                    continue
                }
                val dt = ((nowNs - lastTimeNs) / 1_000_000_000f).coerceIn(0.004f, 0.045f)
                lastTimeNs = nowNs

                val eng = engine ?: continue

                val snap = eng.step(
                    dtSeconds = dt,
                    playerTargetAngle = steeringAngle,
                    playerWantsBoost = wantsBoost,
                    ownerCheats = currentSafeCheats()
                )
                _snapshot.value = snap

                val player = snap.playerSnake
                if (player != null) {
                    // Smooth camera tracking
                    val (cx, cy) = _cameraPos.value
                    _cameraPos.value = (cx + (player.x - cx) * 0.24f) to (cy + (player.y - cy) * 0.24f)

                    if (!player.alive && !sessionRecorded) {
                        sessionRecorded = true
                        recordMatchOutcome(player, eng)
                    }
                }
            }
        }
    }

    private fun recordMatchOutcome(player: ActiveSnake, eng: ArenaEngine) {
        val cfg = currentConfig ?: return
        viewModelScope.launch {
            val res = repository.recordMatchResult(
                uid = currentUserId,
                displayName = cfg.playerName,
                skinId = cfg.skinId,
                pattern = cfg.pattern,
                lengthAchieved = player.score,
                killsEarned = player.kills
            )
            val isNewRecord = res.getOrDefault(false)
            _matchSummary.value = MatchSummary(
                finalLength = player.score,
                peakRank = eng.peakRank.coerceAtLeast(1),
                kills = player.kills,
                survivalSeconds = (eng.survivalMs / 1000L).toInt(),
                eliminatedBy = eng.eliminatedBy ?: "Arena Hazard",
                isNewRecord = isNewRecord
            )
        }
    }

    private fun startMultiplayerSync(room: ArenaRoom) {
        roomObserveJob = viewModelScope.launch {
            repository.observeRoomSnakes(room.id, currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Room snake listener error", firestoreError ?: error)
                }
                .collect { docs ->
                    if (!_isPaused.value) {
                        engine?.updateRemoteSnakes(docs)
                    }
                }
        }

        roomSyncJob = viewModelScope.launch {
            while (isActive) {
                delay(120L)
                if (_isPaused.value) continue
                val snap = _snapshot.value ?: continue
                val isHost = room.hostId == currentUserId

                // Upload local player's snake state
                snap.playerSnake?.let { player ->
                    uploadSnake(room.id, player)
                }

                // Host also uploads AI bot snake states so all joined players see the same bots!
                if (isHost) {
                    val bots = snap.snakes.filter { it.isBot }
                    for (bot in bots) {
                        uploadSnake(room.id, bot)
                    }
                }
            }
        }
    }

    private suspend fun uploadSnake(roomId: String, snake: ActiveSnake) {
        val flattenedPoints = ArrayList<Double>(snake.segments.size * 2)
        for (i in snake.segments.indices) {
            if (flattenedPoints.size >= 158) break
            flattenedPoints.add(snake.segments[i].x.toDouble())
            flattenedPoints.add(snake.segments[i].y.toDouble())
        }
        val wasCreated = snake.id in createdSnakeDocs
        val res = repository.upsertRoomSnake(
            roomId = roomId,
            snakeId = snake.id,
            ownerId = currentUserId,
            displayName = snake.displayName,
            skinId = snake.skinId,
            pattern = snake.pattern,
            botControlled = snake.isBot,
            alive = snake.alive,
            boosting = snake.boosting,
            godMode = snake.godMode,
            x = snake.x.toDouble(),
            y = snake.y.toDouble(),
            angle = snake.angle.toDouble(),
            score = snake.score.toLong(),
            kills = snake.kills.toLong(),
            points = flattenedPoints,
            createdBefore = wasCreated
        )
        if (res.isSuccess) {
            createdSnakeDocs.add(snake.id)
        }
    }

    fun leaveArena() {
        val snap = _snapshot.value
        val eng = engine
        val room = activeRoom
        if (snap?.playerSnake != null && eng != null && !sessionRecorded && snap.playerSnake.alive) {
            sessionRecorded = true
            val cfg = currentConfig
            if (cfg != null) {
                viewModelScope.launch {
                    repository.recordMatchResult(
                        uid = currentUserId,
                        displayName = cfg.playerName,
                        skinId = cfg.skinId,
                        pattern = cfg.pattern,
                        lengthAchieved = snap.playerSnake.score,
                        killsEarned = snap.playerSnake.kills
                    )
                }
            }
        }
        if (room != null) {
            viewModelScope.launch {
                repository.leaveRoom(room.id, currentUserId)
            }
        }
        _activeSession.value = null
        currentConfig = null
        activeRoom = null
        engine = null
        _snapshot.value = null
        _matchSummary.value = null
        _isPaused.value = false
        stopArenaLoopOnly()
    }

    private fun stopArenaLoopOnly() {
        gameLoopJob?.cancel()
        roomSyncJob?.cancel()
        roomObserveJob?.cancel()
        gameLoopJob = null
        roomSyncJob = null
        roomObserveJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopArenaLoopOnly()
    }

    private companion object {
        const val TAG = "GameViewModel"
    }
}
