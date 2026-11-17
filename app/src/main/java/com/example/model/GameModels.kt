package com.example.model

import androidx.compose.ui.graphics.Color

data class Vec2(val x: Float, val y: Float)

enum class SnakePattern(val label: String) {
    SOLID("Solid"),
    STRIPED("Striped"),
    NEON_CORE("Neon Core"),
    DOTS("Polka Pulse");

    companion object {
        fun fromRaw(raw: String?): SnakePattern =
            entries.firstOrNull { it.name == raw } ?: SOLID
    }
}

enum class BotDifficulty(
    val label: String,
    val description: String,
    val reactionIntervalMs: Long,
    val feelerRange: Float,
    val turnRateMultiplier: Float,
    val boostChance: Float,
    val interceptWeight: Float
) {
    EASY(
        label = "Easy",
        description = "Relaxed bots with gentle turns and basic wall avoidance.",
        reactionIntervalMs = 220L,
        feelerRange = 135f,
        turnRateMultiplier = 0.78f,
        boostChance = 0.04f,
        interceptWeight = 0.0f
    ),
    MEDIUM(
        label = "Medium",
        description = "Balanced pathfinding, food seeking, and collision dodging.",
        reactionIntervalMs = 120L,
        feelerRange = 210f,
        turnRateMultiplier = 1.0f,
        boostChance = 0.14f,
        interceptWeight = 0.2f
    ),
    HARD(
        label = "Hard",
        description = "Multi-ray obstacle evasion, loot hunting, and tactical boosts.",
        reactionIntervalMs = 65L,
        feelerRange = 285f,
        turnRateMultiplier = 1.18f,
        boostChance = 0.30f,
        interceptWeight = 0.55f
    ),
    APEX(
        label = "Apex",
        description = "Relentless predictive interceptors with sharp reflexes.",
        reactionIntervalMs = 30L,
        feelerRange = 350f,
        turnRateMultiplier = 1.32f,
        boostChance = 0.45f,
        interceptWeight = 0.85f
    );

    companion object {
        fun fromRaw(raw: String?): BotDifficulty =
            entries.firstOrNull { it.name == raw } ?: MEDIUM
    }
}

enum class PowerUpType(
    val label: String,
    val durationMs: Long,
    val color: Color,
    val accentColor: Color
) {
    SPEED_BOOST(
        label = "Speed Boost",
        durationMs = 8000L,
        color = Color(0xFF00F5D4),
        accentColor = Color(0xFF80FFEA)
    ),
    COIN_MAGNET(
        label = "Coin Magnet",
        durationMs = 9000L,
        color = Color(0xFFFFD166),
        accentColor = Color(0xFFFF2E93)
    )
}

data class SnakeSkin(
    val id: String,
    val name: String,
    val tagline: String,
    val headColor: Color,
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color,
    val eyeColor: Color = Color.White,
    val isPrismatic: Boolean = false,
    val ownerOnly: Boolean = false,
    val unlockCostCoins: Int = 0,
    val dailyRewardDay: Int? = null
)

object SkinCatalog {
    val allSkins: List<SnakeSkin> = listOf(
        SnakeSkin(
            id = "cyber_viper",
            name = "Cyber Viper",
            tagline = "Electric cyan circuit scales",
            headColor = Color(0xFF4DFFEA),
            primaryColor = Color(0xFF00F5D4),
            secondaryColor = Color(0xFF008B8B),
            glowColor = Color(0xFF00F5D4)
        ),
        SnakeSkin(
            id = "plasma_cobra",
            name = "Plasma Cobra",
            tagline = "Hyper-magenta arcade surge",
            headColor = Color(0xFFFF66B2),
            primaryColor = Color(0xFFFF2E93),
            secondaryColor = Color(0xFF7B2CBF),
            glowColor = Color(0xFFFF2E93)
        ),
        SnakeSkin(
            id = "solar_mamba",
            name = "Solar Mamba",
            tagline = "Blazing gold and amber flare",
            headColor = Color(0xFFFFE699),
            primaryColor = Color(0xFFFFD166),
            secondaryColor = Color(0xFFF77F00),
            glowColor = Color(0xFFFFD166)
        ),
        SnakeSkin(
            id = "venom_jade",
            name = "Venom Jade",
            tagline = "Toxic bio-luminescent lime",
            headColor = Color(0xFFB4F8C8),
            primaryColor = Color(0xFF6EEB83),
            secondaryColor = Color(0xFF1B998B),
            glowColor = Color(0xFF6EEB83)
        ),
        SnakeSkin(
            id = "frost_wyrm",
            name = "Frost Wyrm",
            tagline = "Sub-zero arctic ice beam",
            headColor = Color(0xFFE0FBFC),
            primaryColor = Color(0xFF4CC9F0),
            secondaryColor = Color(0xFF3A86FF),
            glowColor = Color(0xFF4CC9F0)
        ),
        SnakeSkin(
            id = "magma_python",
            name = "Magma Python",
            tagline = "Volcanic crimson core",
            headColor = Color(0xFFFF8FA3),
            primaryColor = Color(0xFFFF4D6D),
            secondaryColor = Color(0xFFC9184A),
            glowColor = Color(0xFFFF4D6D)
        ),
        SnakeSkin(
            id = "prismatic_hydra",
            name = "Prismatic Hydra",
            tagline = "Full-spectrum neon wave",
            headColor = Color(0xFFFFFFFF),
            primaryColor = Color(0xFF00F5D4),
            secondaryColor = Color(0xFFFF2E93),
            glowColor = Color(0xFF9B5DE5),
            isPrismatic = true
        ),
        SnakeSkin(
            id = "void_phantom",
            name = "Void Phantom",
            tagline = "Stealth obsidian & ultraviolet",
            headColor = Color(0xFFC77DFF),
            primaryColor = Color(0xFF240046),
            secondaryColor = Color(0xFF9B5DE5),
            glowColor = Color(0xFF9B5DE5)
        ),
        SnakeSkin(
            id = "quantum_anaconda",
            name = "Quantum Anaconda",
            tagline = "Neon vault holographic serpent",
            headColor = Color(0xFFA0FFE6),
            primaryColor = Color(0xFF00F5D4),
            secondaryColor = Color(0xFF7209B7),
            glowColor = Color(0xFF00F5D4),
            unlockCostCoins = 250
        ),
        SnakeSkin(
            id = "chrono_basilisk",
            name = "Chrono Basilisk",
            tagline = "Day 3 Login Reward • Emerald & gold",
            headColor = Color(0xFFFFF3B0),
            primaryColor = Color(0xFF2EC4B6),
            secondaryColor = Color(0xFFFF9F1C),
            glowColor = Color(0xFF2EC4B6),
            dailyRewardDay = 3
        ),
        SnakeSkin(
            id = "nebula_leviathan",
            name = "Nebula Leviathan",
            tagline = "Day 5 Login Reward • Cosmic starlight",
            headColor = Color(0xFFF72585),
            primaryColor = Color(0xFF4361EE),
            secondaryColor = Color(0xFF7209B7),
            glowColor = Color(0xFFF72585),
            dailyRewardDay = 5
        ),
        SnakeSkin(
            id = "solar_eclipse",
            name = "Solar Eclipse",
            tagline = "Day 7 Streak Jackpot • Corona prism",
            headColor = Color(0xFFFFD166),
            primaryColor = Color(0xFFFF5400),
            secondaryColor = Color(0xFF1A1423),
            glowColor = Color(0xFFFF9E00),
            isPrismatic = true,
            dailyRewardDay = 7
        ),
        SnakeSkin(
            id = "apex_sovereign",
            name = "Apex Sovereign",
            tagline = "Owner-exclusive gilded prism",
            headColor = Color(0xFFFFF3B0),
            primaryColor = Color(0xFFFFD166),
            secondaryColor = Color(0xFF00F5D4),
            glowColor = Color(0xFFFFD166),
            isPrismatic = true,
            ownerOnly = true
        )
    )

    fun getById(id: String): SnakeSkin =
        allSkins.firstOrNull { it.id == id } ?: allSkins.first()

    fun availableFor(isOwner: Boolean): List<SnakeSkin> =
        if (isOwner) allSkins else allSkins.filterNot { it.ownerOnly }

    fun isSkinUnlocked(skin: SnakeSkin, unlockedSkinIds: List<String>, isOwner: Boolean): Boolean {
        if (skin.ownerOnly) return isOwner
        if (skin.unlockCostCoins == 0 && skin.dailyRewardDay == null) return true
        return isOwner || skin.id in unlockedSkinIds
    }
}

data class FoodOrb(
    val id: Int,
    val x: Float,
    val y: Float,
    val value: Int,
    val color: Color,
    val radius: Float,
    val isLootDrop: Boolean = false
)

data class PowerUpOrb(
    val id: Int,
    val type: PowerUpType,
    val x: Float,
    val y: Float,
    val spawnedAtMs: Long
)

data class ActiveSnake(
    val id: String,
    val displayName: String,
    val skinId: String,
    val pattern: SnakePattern,
    val isPlayer: Boolean,
    val isBot: Boolean,
    val alive: Boolean = true,
    val boosting: Boolean = false,
    val godMode: Boolean = false,
    val x: Float,
    val y: Float,
    val angle: Float,
    val targetAngle: Float,
    val score: Int = 25,
    val kills: Int = 0,
    val segments: List<Vec2> = emptyList(),
    val speedBoostRemainingMs: Long = 0L,
    val magnetRemainingMs: Long = 0L,
    val botThinkCooldownMs: Long = 0L,
    val botWanderAngle: Float = angle,
    val respawnTimerMs: Long = 0L
) {
    val bodyRadius: Float
        get() = (13f + kotlin.math.sqrt(score.toFloat()) * 0.42f).coerceIn(13f, 28f)

    val desiredSegmentCount: Int
        get() = (10 + (score / 4)).coerceIn(10, 80)
}

data class OwnerCheatState(
    val godMode: Boolean = false,
    val permaSpeedOverdrive: Boolean = false,
    val permaMegaMagnet: Boolean = false,
    val freezeBots: Boolean = false,
    val botDifficultyOverride: BotDifficulty? = null
)

data class ArenaHudEntry(
    val id: String,
    val name: String,
    val length: Int,
    val isPlayer: Boolean,
    val isBot: Boolean,
    val skinId: String
)

data class MatchSummary(
    val finalLength: Int,
    val peakRank: Int,
    val kills: Int,
    val survivalSeconds: Int,
    val eliminatedBy: String,
    val isNewRecord: Boolean
)

enum class SoundEffect {
    EAT_ORB,
    EAT_LOOT,
    BOOST_START,
    POWERUP_SPEED,
    POWERUP_MAGNET,
    ELIMINATION_KILL,
    PLAYER_CRASH,
    UI_CLICK,
    REWARD_CLAIM
}

enum class HapticEvent {
    FOOD_ORB,
    FOOD_LOOT,
    BOOST_START,
    BOOST_PULSE,
    POWERUP_PICKUP,
    ELIMINATION_KILL,
    PLAYER_CRASH
}

data class GameSettings(
    val showMinimap: Boolean = true,
    val showNameplates: Boolean = true,
    val showJoystickRing: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val soundEffectsEnabled: Boolean = true
)

enum class DailyRewardKind {
    COINS,
    SKIN
}

data class DailyRewardTier(
    val day: Int,
    val title: String,
    val kind: DailyRewardKind,
    val coinsAmount: Int,
    val skinId: String? = null
)

data class DailyRewardStatus(
    val currentStreak: Int,
    val nextRewardDay: Int,
    val canClaimNow: Boolean,
    val remainingCooldownMs: Long,
    val lastClaimEpochMs: Long?
)

data class DailyClaimResult(
    val claimedDay: Int,
    val coinsAwarded: Int,
    val unlockedSkinId: String?,
    val newBalance: Long,
    val newStreak: Int
)

object DailyRewardCatalog {
    const val COOLDOWN_MS: Long = 24L * 60L * 60L * 1000L

    val tiers: List<DailyRewardTier> = listOf(
        DailyRewardTier(1, "100 Coins", DailyRewardKind.COINS, coinsAmount = 100),
        DailyRewardTier(2, "150 Coins", DailyRewardKind.COINS, coinsAmount = 150),
        DailyRewardTier(3, "Chrono Basilisk", DailyRewardKind.SKIN, coinsAmount = 50, skinId = "chrono_basilisk"),
        DailyRewardTier(4, "250 Coins", DailyRewardKind.COINS, coinsAmount = 250),
        DailyRewardTier(5, "Nebula Leviathan", DailyRewardKind.SKIN, coinsAmount = 100, skinId = "nebula_leviathan"),
        DailyRewardTier(6, "400 Coins", DailyRewardKind.COINS, coinsAmount = 400),
        DailyRewardTier(7, "Solar Eclipse", DailyRewardKind.SKIN, coinsAmount = 500, skinId = "solar_eclipse")
    )

    fun tierForDay(day: Int): DailyRewardTier {
        val normalized = ((day - 1).mod(tiers.size)) + 1
        return tiers.first { it.day == normalized }
    }

    fun computeStatus(
        currentStreak: Int,
        lastClaimEpochMs: Long?,
        nowEpochMs: Long = System.currentTimeMillis()
    ): DailyRewardStatus {
        val clampedStreak = currentStreak.coerceIn(0, 7)
        if (lastClaimEpochMs == null || lastClaimEpochMs <= 0L) {
            return DailyRewardStatus(
                currentStreak = clampedStreak,
                nextRewardDay = 1,
                canClaimNow = true,
                remainingCooldownMs = 0L,
                lastClaimEpochMs = null
            )
        }
        val elapsed = (nowEpochMs - lastClaimEpochMs).coerceAtLeast(0L)
        val remaining = (COOLDOWN_MS - elapsed).coerceAtLeast(0L)
        val canClaim = remaining == 0L
        // Reset streak if missed more than 48h, otherwise advance from 1..7 and wrap to 1 after Day 7
        val effectiveStreak = if (elapsed > COOLDOWN_MS * 2L && clampedStreak > 0) 0 else clampedStreak
        val nextDay = if (effectiveStreak >= 7) 1 else (effectiveStreak + 1).coerceIn(1, 7)
        return DailyRewardStatus(
            currentStreak = effectiveStreak,
            nextRewardDay = nextDay,
            canClaimNow = canClaim,
            remainingCooldownMs = remaining,
            lastClaimEpochMs = lastClaimEpochMs
        )
    }

    fun formatCountdown(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0L) + 999L) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds)
    }
}
