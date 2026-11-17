package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.model.ActiveSnake
import com.example.model.ArenaHudEntry
import com.example.model.BotDifficulty
import com.example.model.FoodOrb
import com.example.model.HapticEvent
import com.example.model.OwnerCheatState
import com.example.model.PowerUpOrb
import com.example.model.PowerUpType
import com.example.model.RemoteSnakeDoc
import com.example.model.SkinCatalog
import com.example.model.SnakePattern
import com.example.model.SoundEffect
import com.example.model.Vec2
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

data class ArenaSnapshot(
    val snakes: List<ActiveSnake>,
    val foodOrbs: List<FoodOrb>,
    val powerUps: List<PowerUpOrb>,
    val hudLeaderboard: List<ArenaHudEntry>,
    val playerRank: Int,
    val totalAliveCount: Int,
    val playerSnake: ActiveSnake?,
    val recentBannerEvent: String?,
    val soundEvents: List<SoundEffect> = emptyList(),
    val hapticEvents: List<HapticEvent> = emptyList()
)

class ArenaEngine(
    private val playerUid: String,
    private val playerName: String,
    private val playerSkinId: String,
    private val playerPattern: SnakePattern,
    private val initialBotCount: Int,
    private val initialDifficulty: BotDifficulty,
    private val isHostOrSolo: Boolean
) {
    companion object {
        const val WORLD_SIZE = 2600f
        const val TARGET_AMBIENT_FOOD = 220
        const val TARGET_POWER_UPS = 9
        const val BASE_SPEED = 175f
        const val BOOST_SPEED_MULTIPLIER = 1.72f
        const val POWERUP_SPEED_MULTIPLIER = 1.62f
        const val BASE_TURN_RATE = 4.3f
        const val SEGMENT_SPACING = 11.5f
        const val BASE_MAGNET_PULL_RADIUS = 38f
        const val POWERUP_MAGNET_RADIUS = 270f
        const val OWNER_MEGA_MAGNET_RADIUS = 460f

        private val FOOD_COLORS = listOf(
            Color(0xFF00F5D4),
            Color(0xFFFF2E93),
            Color(0xFFFFD166),
            Color(0xFF6EEB83),
            Color(0xFF4CC9F0),
            Color(0xFFC77DFF)
        )

        private val BOT_NAMES = listOf(
            "HexByte", "NovaFang", "VoltCoil", "Krypton", "PulseScale",
            "Spectre", "AstraWorm", "ShadowFin", "Hyperion", "Zephyr",
            "NyxViper", "Cobalt"
        )
    }

    private var nextOrbId = 1
    private var nextPowerUpId = 1
    private var boostDrainAccumulator = mutableMapOf<String, Float>()
    private var powerUpSpawnTimerMs = 0L
    private var boostHapticTimerMs = 0L
    private var bannerEvent: String? = null
    private var bannerTimerMs = 0L

    var botDifficulty: BotDifficulty = initialDifficulty
        private set

    var botCount: Int = initialBotCount.coerceIn(0, 12)
        private set

    private val localSnakes = mutableListOf<ActiveSnake>()
    private val remoteSnakesMap = mutableMapOf<String, ActiveSnake>()
    private val foodOrbs = mutableListOf<FoodOrb>()
    private val powerUps = mutableListOf<PowerUpOrb>()

    var peakRank: Int = 99
        private set
    var survivalMs: Long = 0L
        private set
    var eliminatedBy: String? = null
        private set

    init {
        resetMatch(playerSkinId, playerPattern, playerName)
    }

    fun resetMatch(skinId: String, pattern: SnakePattern, displayName: String) {
        localSnakes.clear()
        foodOrbs.clear()
        powerUps.clear()
        boostDrainAccumulator.clear()
        survivalMs = 0L
        peakRank = 99
        eliminatedBy = null
        boostHapticTimerMs = 0L
        bannerEvent = null
        bannerTimerMs = 0L

        // Spawn human player near center with safe clearance
        val startX = WORLD_SIZE * 0.5f + (Random.nextFloat() - 0.5f) * 500f
        val startY = WORLD_SIZE * 0.5f + (Random.nextFloat() - 0.5f) * 500f
        val startAngle = Random.nextFloat() * (2f * PI.toFloat()) - PI.toFloat()
        localSnakes.add(
            createSnake(
                id = playerUid,
                name = displayName,
                skinId = skinId,
                pattern = pattern,
                isPlayer = true,
                isBot = false,
                x = startX,
                y = startY,
                angle = startAngle,
                initialScore = 25
            )
        )

        // Spawn AI Bots if solo or room host
        if (isHostOrSolo) {
            for (i in 0 until botCount) {
                localSnakes.add(spawnBotSnake(i))
            }
        }

        repeat(TARGET_AMBIENT_FOOD) {
            foodOrbs.add(spawnAmbientFood())
        }
        repeat(TARGET_POWER_UPS) {
            powerUps.add(spawnPowerUp(System.currentTimeMillis()))
        }
    }

    fun updateDifficulty(newDifficulty: BotDifficulty) {
        botDifficulty = newDifficulty
        pushBanner("Bot AI set to ${newDifficulty.label}")
    }

    fun updateBotCount(count: Int) {
        val target = count.coerceIn(0, 12)
        botCount = target
        if (!isHostOrSolo) return
        val currentBots = localSnakes.filter { it.isBot }
        if (currentBots.size < target) {
            for (i in currentBots.size until target) {
                localSnakes.add(spawnBotSnake(i))
            }
        } else if (currentBots.size > target) {
            val toRemove = currentBots.drop(target).map { it.id }.toSet()
            localSnakes.removeAll { it.id in toRemove }
        }
    }

    fun applyOwnerLengthBoost(delta: Int) {
        val idx = localSnakes.indexOfFirst { it.isPlayer && it.alive }
        if (idx >= 0) {
            val p = localSnakes[idx]
            localSnakes[idx] = p.copy(score = (p.score + delta).coerceAtMost(999_999))
            pushBanner("OWNER SURGE: +$delta LENGTH!")
        }
    }

    fun spawnOwnerFoodRain() {
        val p = localSnakes.firstOrNull { it.isPlayer } ?: return
        repeat(40) {
            val a = Random.nextFloat() * 2f * PI.toFloat()
            val r = 55f + Random.nextFloat() * 220f
            val fx = (p.x + cos(a) * r).coerceIn(30f, WORLD_SIZE - 30f)
            val fy = (p.y + sin(a) * r).coerceIn(30f, WORLD_SIZE - 30f)
            foodOrbs.add(
                FoodOrb(
                    id = nextOrbId++,
                    x = fx,
                    y = fy,
                    value = 8,
                    color = Color(0xFFFFD166),
                    radius = 10f,
                    isLootDrop = true
                )
            )
        }
        pushBanner("OWNER: GOLDEN FOOD RAIN!")
    }

    fun eliminateAllBots() {
        var count = 0
        for (i in localSnakes.indices) {
            val s = localSnakes[i]
            if (s.isBot && s.alive) {
                dropSnakeLoot(s)
                localSnakes[i] = s.copy(alive = false, respawnTimerMs = 4000L)
                count++
            }
        }
        val playerIdx = localSnakes.indexOfFirst { it.isPlayer && it.alive }
        if (playerIdx >= 0 && count > 0) {
            val p = localSnakes[playerIdx]
            localSnakes[playerIdx] = p.copy(kills = p.kills + count)
        }
        pushBanner("OWNER SMITE: $count BOTS ELIMINATED!")
    }

    fun updateRemoteSnakes(docs: List<RemoteSnakeDoc>) {
        val incomingIds = mutableSetOf<String>()
        for (doc in docs) {
            if (doc.id == playerUid) continue
            if (isHostOrSolo && doc.botControlled && doc.ownerId == playerUid) continue
            incomingIds.add(doc.id)

            val segs = mutableListOf<Vec2>()
            val pts = doc.points
            var i = 0
            while (i + 1 < pts.size) {
                segs.add(Vec2(pts[i].toFloat(), pts[i + 1].toFloat()))
                i += 2
            }
            if (segs.isEmpty()) {
                segs.add(Vec2(doc.x.toFloat(), doc.y.toFloat()))
            }

            remoteSnakesMap[doc.id] = ActiveSnake(
                id = doc.id,
                displayName = doc.displayName,
                skinId = doc.skinId,
                pattern = SnakePattern.fromRaw(doc.pattern),
                isPlayer = false,
                isBot = doc.botControlled,
                alive = doc.alive,
                boosting = doc.boosting,
                godMode = doc.godMode,
                x = doc.x.toFloat(),
                y = doc.y.toFloat(),
                angle = doc.angle.toFloat(),
                targetAngle = doc.angle.toFloat(),
                score = doc.score.toInt(),
                kills = doc.kills.toInt(),
                segments = segs
            )
        }
        val stale = remoteSnakesMap.keys.filter { it !in incomingIds }
        stale.forEach { remoteSnakesMap.remove(it) }
    }

    fun step(
        dtSeconds: Float,
        playerTargetAngle: Float?,
        playerWantsBoost: Boolean,
        ownerCheats: OwnerCheatState
    ): ArenaSnapshot {
        val dt = dtSeconds.coerceIn(0.001f, 0.05f)
        val dtMs = (dt * 1000f).toLong().coerceAtLeast(1L)
        val effectiveDifficulty = ownerCheats.botDifficultyOverride ?: botDifficulty
        val frameSounds = mutableListOf<SoundEffect>()
        val frameHaptics = mutableListOf<HapticEvent>()

        if (bannerTimerMs > 0L) {
            bannerTimerMs = (bannerTimerMs - dtMs).coerceAtLeast(0L)
            if (bannerTimerMs == 0L) bannerEvent = null
        }

        // Smooth dead-reckoning for remote snakes between Firestore snapshots
        for ((id, remote) in remoteSnakesMap) {
            if (remote.alive) {
                val speed = BASE_SPEED * (if (remote.boosting) BOOST_SPEED_MULTIPLIER else 1f)
                val nx = (remote.x + cos(remote.angle) * speed * dt).coerceIn(10f, WORLD_SIZE - 10f)
                val ny = (remote.y + sin(remote.angle) * speed * dt).coerceIn(10f, WORLD_SIZE - 10f)
                val updatedSegs = followSegments(nx, ny, remote.segments, remote.desiredSegmentCount)
                remoteSnakesMap[id] = remote.copy(x = nx, y = ny, segments = updatedSegs)
            }
        }

        val allVisibleSnakes = localSnakes + remoteSnakesMap.values

        // Update local snakes (Player + local Bots)
        for (i in localSnakes.indices) {
            var snake = localSnakes[i]

            if (!snake.alive) {
                if (snake.isBot && isHostOrSolo) {
                    val remaining = snake.respawnTimerMs - dtMs
                    if (remaining <= 0L) {
                        localSnakes[i] = spawnBotSnake(i)
                    } else {
                        localSnakes[i] = snake.copy(respawnTimerMs = remaining)
                    }
                }
                continue
            }

            if (snake.isPlayer) {
                survivalMs += dtMs
            }

            // Decrement power-up timers
            val nextSpeedPu = (snake.speedBoostRemainingMs - dtMs).coerceAtLeast(0L)
            val nextMagnetPu = (snake.magnetRemainingMs - dtMs).coerceAtLeast(0L)

            // Determine steering & boost
            var nextTargetAngle = snake.targetAngle
            var nextWander = snake.botWanderAngle
            var nextThinkCooldown = snake.botThinkCooldownMs
            var isBoosting = false
            val isGod = snake.isPlayer && ownerCheats.godMode

            if (snake.isPlayer) {
                if (playerTargetAngle != null) {
                    nextTargetAngle = playerTargetAngle
                }
                isBoosting = (playerWantsBoost && snake.score > 16) || ownerCheats.permaSpeedOverdrive
                if (isBoosting) {
                    if (!snake.boosting) {
                        frameSounds.add(SoundEffect.BOOST_START)
                        frameHaptics.add(HapticEvent.BOOST_START)
                        boostHapticTimerMs = 360L
                    } else {
                        boostHapticTimerMs -= dtMs
                        if (boostHapticTimerMs <= 0L) {
                            frameHaptics.add(HapticEvent.BOOST_PULSE)
                            boostHapticTimerMs = 360L
                        }
                    }
                } else {
                    boostHapticTimerMs = 0L
                }
            } else if (snake.isBot) {
                if (ownerCheats.freezeBots) {
                    localSnakes[i] = snake.copy(
                        speedBoostRemainingMs = nextSpeedPu,
                        magnetRemainingMs = nextMagnetPu,
                        boosting = false
                    )
                    continue
                }
                nextThinkCooldown -= dtMs
                if (nextThinkCooldown <= 0L) {
                    val decision = BotAiController.decide(
                        bot = snake,
                        difficulty = effectiveDifficulty,
                        allSnakes = allVisibleSnakes,
                        foodOrbs = foodOrbs,
                        powerUps = powerUps,
                        worldSize = WORLD_SIZE
                    )
                    nextTargetAngle = decision.targetAngle
                    isBoosting = decision.shouldBoost
                    nextWander = decision.updatedWanderAngle
                    nextThinkCooldown = effectiveDifficulty.reactionIntervalMs
                } else {
                    isBoosting = snake.boosting && snake.score > 20
                }
            }

            // Smooth turn toward target angle
            val turnMultiplier = if (snake.isBot) effectiveDifficulty.turnRateMultiplier else 1.15f
            val maxTurn = BASE_TURN_RATE * turnMultiplier * dt
            val angleDiff = BotAiController.normalizeAngle(nextTargetAngle - snake.angle)
            val newAngle = BotAiController.normalizeAngle(
                snake.angle + angleDiff.coerceIn(-maxTurn, maxTurn)
            )

            // Calculate movement speed
            var speedMult = 1.0f
            if (nextSpeedPu > 0L) speedMult *= POWERUP_SPEED_MULTIPLIER
            if (isBoosting) {
                speedMult *= if (snake.isPlayer && ownerCheats.permaSpeedOverdrive) 2.0f else BOOST_SPEED_MULTIPLIER
            }
            val stepDist = BASE_SPEED * speedMult * dt
            var nx = snake.x + cos(newAngle) * stepDist
            var ny = snake.y + sin(newAngle) * stepDist

            // Manual boost drains mass slightly and drops small orbs behind tail
            var nextScore = snake.score
            val manualDrain = isBoosting && nextSpeedPu == 0L && !(snake.isPlayer && ownerCheats.permaSpeedOverdrive)
            if (manualDrain && nextScore > 16) {
                val acc = (boostDrainAccumulator[snake.id] ?: 0f) + dt * 5.2f
                if (acc >= 1f) {
                    val lose = acc.toInt()
                    nextScore = (nextScore - lose).coerceAtLeast(16)
                    boostDrainAccumulator[snake.id] = acc - lose
                    val tail = snake.segments.lastOrNull() ?: Vec2(snake.x, snake.y)
                    foodOrbs.add(
                        FoodOrb(
                            id = nextOrbId++,
                            x = tail.x + (Random.nextFloat() - 0.5f) * 14f,
                            y = tail.y + (Random.nextFloat() - 0.5f) * 14f,
                            value = 1,
                            color = SkinCatalog.getById(snake.skinId).primaryColor,
                            radius = 5.5f
                        )
                    )
                } else {
                    boostDrainAccumulator[snake.id] = acc
                }
            }

            // Boundary check
            var finalAngle = newAngle
            if (nx < 16f || nx > WORLD_SIZE - 16f || ny < 16f || ny > WORLD_SIZE - 16f) {
                if (isGod) {
                    // God mode bounces safely off walls
                    nx = nx.coerceIn(22f, WORLD_SIZE - 22f)
                    ny = ny.coerceIn(22f, WORLD_SIZE - 22f)
                    finalAngle = atan2(WORLD_SIZE * 0.5f - ny, WORLD_SIZE * 0.5f - nx)
                } else {
                    dropSnakeLoot(snake)
                    localSnakes[i] = snake.copy(
                        alive = false,
                        boosting = false,
                        respawnTimerMs = if (snake.isBot) 2500L else 0L
                    )
                    if (snake.isPlayer) {
                        eliminatedBy = "Arena Laser Wall"
                        frameSounds.add(SoundEffect.PLAYER_CRASH)
                        frameHaptics.add(HapticEvent.PLAYER_CRASH)
                    }
                    continue
                }
            }

            val updatedSegments = followSegments(nx, ny, snake.segments, snake.desiredSegmentCount)
            snake = snake.copy(
                x = nx,
                y = ny,
                angle = finalAngle,
                targetAngle = nextTargetAngle,
                boosting = isBoosting,
                godMode = isGod,
                score = nextScore,
                segments = updatedSegments,
                speedBoostRemainingMs = nextSpeedPu,
                magnetRemainingMs = nextMagnetPu,
                botThinkCooldownMs = nextThinkCooldown,
                botWanderAngle = nextWander
            )

            // Food consumption & Coin Magnet attraction
            val magnetActive = snake.magnetRemainingMs > 0L || (snake.isPlayer && ownerCheats.permaMegaMagnet)
            val attractRadius = when {
                snake.isPlayer && ownerCheats.permaMegaMagnet -> OWNER_MEGA_MAGNET_RADIUS
                snake.magnetRemainingMs > 0L -> POWERUP_MAGNET_RADIUS
                else -> BASE_MAGNET_PULL_RADIUS + snake.bodyRadius
            }
            val eatRadius = snake.bodyRadius + 14f

            var gainedScore = 0
            var ateLoot = false
            val orbIter = foodOrbs.listIterator()
            while (orbIter.hasNext()) {
                val orb = orbIter.next()
                val dx = snake.x - orb.x
                val dy = snake.y - orb.y
                val dist = hypot(dx, dy)
                if (dist <= eatRadius + orb.radius) {
                    gainedScore += orb.value
                    if (orb.isLootDrop || orb.value >= 6) ateLoot = true
                    orbIter.remove()
                } else if (dist <= attractRadius && dist > 1f) {
                    val pullSpeed = if (magnetActive) 440f else 180f
                    val move = (pullSpeed * dt).coerceAtMost(dist)
                    orbIter.set(
                        orb.copy(
                            x = orb.x + (dx / dist) * move,
                            y = orb.y + (dy / dist) * move
                        )
                    )
                }
            }
            if (gainedScore > 0) {
                snake = snake.copy(score = (snake.score + gainedScore).coerceAtMost(999_999))
                if (snake.isPlayer) {
                    frameSounds.add(if (ateLoot) SoundEffect.EAT_LOOT else SoundEffect.EAT_ORB)
                    frameHaptics.add(if (ateLoot) HapticEvent.FOOD_LOOT else HapticEvent.FOOD_ORB)
                }
            }

            // Power-Up pickups
            val puIter = powerUps.listIterator()
            while (puIter.hasNext()) {
                val pu = puIter.next()
                val dist = hypot(snake.x - pu.x, snake.y - pu.y)
                if (dist <= snake.bodyRadius + 24f) {
                    when (pu.type) {
                        PowerUpType.SPEED_BOOST -> {
                            snake = snake.copy(speedBoostRemainingMs = PowerUpType.SPEED_BOOST.durationMs)
                            if (snake.isPlayer) {
                                pushBanner("SPEED BOOST ACTIVE (8s)!")
                                frameSounds.add(SoundEffect.POWERUP_SPEED)
                                frameHaptics.add(HapticEvent.POWERUP_PICKUP)
                            }
                        }
                        PowerUpType.COIN_MAGNET -> {
                            snake = snake.copy(magnetRemainingMs = PowerUpType.COIN_MAGNET.durationMs)
                            if (snake.isPlayer) {
                                pushBanner("COIN MAGNET ACTIVE (9s)!")
                                frameSounds.add(SoundEffect.POWERUP_MAGNET)
                                frameHaptics.add(HapticEvent.POWERUP_PICKUP)
                            }
                        }
                    }
                    puIter.remove()
                }
            }

            localSnakes[i] = snake
        }

        // Snake-to-snake head collision checks
        val currentSnapshotList = localSnakes + remoteSnakesMap.values
        val killerCreditIds = mutableListOf<String>()
        for (i in localSnakes.indices) {
            val snake = localSnakes[i]
            if (!snake.alive || snake.godMode) continue

            for (other in currentSnapshotList) {
                if (!other.alive || other.id == snake.id) continue

                // Check head vs other's body segments
                var hit = false
                for (segIdx in other.segments.indices) {
                    val seg = other.segments[segIdx]
                    val dist = hypot(snake.x - seg.x, snake.y - seg.y)
                    val hitRadius = snake.bodyRadius * 0.72f + other.bodyRadius * 0.78f
                    if (dist < hitRadius) {
                        // Head-on collision: larger snake survives unless equal
                        if (segIdx == 0 && snake.score > other.score) {
                            continue
                        }
                        hit = true
                        break
                    }
                }
                if (hit) {
                    dropSnakeLoot(snake)
                    localSnakes[i] = snake.copy(
                        alive = false,
                        boosting = false,
                        respawnTimerMs = if (snake.isBot) 2500L else 0L
                    )
                    killerCreditIds.add(other.id)
                    if (snake.isPlayer) {
                        eliminatedBy = other.displayName
                        frameSounds.add(SoundEffect.PLAYER_CRASH)
                        frameHaptics.add(HapticEvent.PLAYER_CRASH)
                    } else if (other.isPlayer) {
                        pushBanner("ELIMINATED ${snake.displayName.uppercase()}!")
                        frameSounds.add(SoundEffect.ELIMINATION_KILL)
                        frameHaptics.add(HapticEvent.ELIMINATION_KILL)
                    }
                    break
                }
            }
        }

        for (killerId in killerCreditIds) {
            val kIdx = localSnakes.indexOfFirst { it.id == killerId && it.alive }
            if (kIdx >= 0) {
                val k = localSnakes[kIdx]
                localSnakes[kIdx] = k.copy(kills = k.kills + 1)
            }
        }

        // Maintain ambient food & power-ups
        while (foodOrbs.size < TARGET_AMBIENT_FOOD) {
            foodOrbs.add(spawnAmbientFood())
        }
        powerUpSpawnTimerMs += dtMs
        if (powerUps.size < TARGET_POWER_UPS && powerUpSpawnTimerMs >= 2500L) {
            powerUpSpawnTimerMs = 0L
            powerUps.add(spawnPowerUp(System.currentTimeMillis()))
        }

        val combinedSnakes = localSnakes + remoteSnakesMap.values
        val aliveSnakes = combinedSnakes.filter { it.alive }.sortedByDescending { it.score }
        val player = localSnakes.firstOrNull { it.isPlayer }
        val rankIdx = aliveSnakes.indexOfFirst { it.id == playerUid }
        val currentRank = if (rankIdx >= 0) rankIdx + 1 else (aliveSnakes.size + 1)
        if (player?.alive == true && currentRank < peakRank) {
            peakRank = currentRank
        }

        val hudEntries = aliveSnakes.take(5).map { s ->
            ArenaHudEntry(
                id = s.id,
                name = s.displayName,
                length = s.score,
                isPlayer = s.isPlayer,
                isBot = s.isBot,
                skinId = s.skinId
            )
        }

        return ArenaSnapshot(
            snakes = combinedSnakes,
            foodOrbs = foodOrbs.toList(),
            powerUps = powerUps.toList(),
            hudLeaderboard = hudEntries,
            playerRank = currentRank,
            totalAliveCount = aliveSnakes.size,
            playerSnake = player,
            recentBannerEvent = bannerEvent,
            soundEvents = frameSounds.distinct(),
            hapticEvents = frameHaptics.distinct()
        )
    }

    private fun pushBanner(message: String) {
        bannerEvent = message
        bannerTimerMs = 2400L
    }

    private fun createSnake(
        id: String,
        name: String,
        skinId: String,
        pattern: SnakePattern,
        isPlayer: Boolean,
        isBot: Boolean,
        x: Float,
        y: Float,
        angle: Float,
        initialScore: Int
    ): ActiveSnake {
        val segCount = (10 + initialScore / 4).coerceIn(10, 80)
        val segs = List(segCount) { idx ->
            Vec2(
                x - cos(angle) * idx * SEGMENT_SPACING,
                y - sin(angle) * idx * SEGMENT_SPACING
            )
        }
        return ActiveSnake(
            id = id,
            displayName = name,
            skinId = skinId,
            pattern = pattern,
            isPlayer = isPlayer,
            isBot = isBot,
            alive = true,
            x = x,
            y = y,
            angle = angle,
            targetAngle = angle,
            score = initialScore,
            segments = segs
        )
    }

    private fun spawnBotSnake(index: Int): ActiveSnake {
        val margin = 260f
        val bx = margin + Random.nextFloat() * (WORLD_SIZE - margin * 2f)
        val by = margin + Random.nextFloat() * (WORLD_SIZE - margin * 2f)
        val angle = Random.nextFloat() * 2f * PI.toFloat() - PI.toFloat()
        val skins = SkinCatalog.availableFor(isOwner = false)
        val skin = skins[(index + Random.nextInt(skins.size)) % skins.size]
        val pattern = SnakePattern.entries[Random.nextInt(SnakePattern.entries.size)]
        val botName = BOT_NAMES[index % BOT_NAMES.size]
        val startScore = when (botDifficulty) {
            BotDifficulty.EASY -> Random.nextInt(20, 45)
            BotDifficulty.MEDIUM -> Random.nextInt(30, 75)
            BotDifficulty.HARD -> Random.nextInt(45, 110)
            BotDifficulty.APEX -> Random.nextInt(65, 150)
        }
        return createSnake(
            id = "bot_$index",
            name = botName,
            skinId = skin.id,
            pattern = pattern,
            isPlayer = false,
            isBot = true,
            x = bx,
            y = by,
            angle = angle,
            initialScore = startScore
        )
    }

    private fun spawnAmbientFood(): FoodOrb {
        val roll = Random.nextFloat()
        val value = when {
            roll > 0.93f -> 6
            roll > 0.72f -> 3
            else -> 1
        }
        val radius = when (value) {
            6 -> 9.5f
            3 -> 7.2f
            else -> 5.2f
        }
        val color = if (value == 6) Color(0xFFFFD166) else FOOD_COLORS[Random.nextInt(FOOD_COLORS.size)]
        return FoodOrb(
            id = nextOrbId++,
            x = 55f + Random.nextFloat() * (WORLD_SIZE - 110f),
            y = 55f + Random.nextFloat() * (WORLD_SIZE - 110f),
            value = value,
            color = color,
            radius = radius
        )
    }

    private fun spawnPowerUp(nowMs: Long): PowerUpOrb {
        val type = if (Random.nextBoolean()) PowerUpType.SPEED_BOOST else PowerUpType.COIN_MAGNET
        return PowerUpOrb(
            id = nextPowerUpId++,
            type = type,
            x = 180f + Random.nextFloat() * (WORLD_SIZE - 360f),
            y = 180f + Random.nextFloat() * (WORLD_SIZE - 360f),
            spawnedAtMs = nowMs
        )
    }

    private fun dropSnakeLoot(snake: ActiveSnake) {
        val skinColor = SkinCatalog.getById(snake.skinId).primaryColor
        val orbValue = (snake.score / snake.segments.size.coerceAtLeast(1)).coerceIn(3, 12)
        for (i in snake.segments.indices step 2) {
            val seg = snake.segments[i]
            foodOrbs.add(
                FoodOrb(
                    id = nextOrbId++,
                    x = (seg.x + (Random.nextFloat() - 0.5f) * 18f).coerceIn(24f, WORLD_SIZE - 24f),
                    y = (seg.y + (Random.nextFloat() - 0.5f) * 18f).coerceIn(24f, WORLD_SIZE - 24f),
                    value = orbValue,
                    color = skinColor,
                    radius = 9.5f,
                    isLootDrop = true
                )
            )
        }
    }

    private fun followSegments(
        headX: Float,
        headY: Float,
        oldSegments: List<Vec2>,
        desiredCount: Int
    ): List<Vec2> {
        val result = ArrayList<Vec2>(desiredCount)
        result.add(Vec2(headX, headY))

        var prevX = headX
        var prevY = headY
        for (i in 1 until desiredCount) {
            val source = if (i < oldSegments.size) oldSegments[i] else result.last()
            val dx = source.x - prevX
            val dy = source.y - prevY
            val dist = hypot(dx, dy)
            val placed = if (dist > 0.001f) {
                Vec2(
                    prevX + (dx / dist) * SEGMENT_SPACING,
                    prevY + (dy / dist) * SEGMENT_SPACING
                )
            } else {
                Vec2(prevX, prevY)
            }
            result.add(placed)
            prevX = placed.x
            prevY = placed.y
        }
        return result
    }
}
