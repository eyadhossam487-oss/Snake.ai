package com.example

import com.example.game.ArenaEngine
import com.example.model.BotDifficulty
import com.example.model.DailyRewardCatalog
import com.example.model.DailyRewardKind
import com.example.model.GameSettings
import com.example.model.HapticEvent
import com.example.model.OwnerCheatState
import com.example.model.PowerUpType
import com.example.model.SkinCatalog
import com.example.model.SnakePattern
import com.example.model.SoundEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun arenaEngine_spawnsPlayerBotsAndFood() {
        val engine = ArenaEngine(
            playerUid = "u1",
            playerName = "TestViper",
            playerSkinId = "cyber_viper",
            playerPattern = SnakePattern.SOLID,
            initialBotCount = 5,
            initialDifficulty = BotDifficulty.HARD,
            isHostOrSolo = true
        )
        val snap = engine.step(0.016f, 0.2f, false, OwnerCheatState())
        assertEquals(6, snap.snakes.size)
        assertTrue(snap.foodOrbs.size >= 200)
        assertTrue(snap.powerUps.isNotEmpty())
        assertEquals(8000L, PowerUpType.SPEED_BOOST.durationMs)
        assertEquals(9000L, PowerUpType.COIN_MAGNET.durationMs)
    }

    @Test
    fun ownerCheats_boostLengthAndSmiteBots() {
        val engine = ArenaEngine(
            playerUid = "owner_1",
            playerName = "Owner",
            playerSkinId = "apex_sovereign",
            playerPattern = SnakePattern.NEON_CORE,
            initialBotCount = 4,
            initialDifficulty = BotDifficulty.MEDIUM,
            isHostOrSolo = true
        )
        engine.applyOwnerLengthBoost(500)
        engine.eliminateAllBots()
        val snap = engine.step(0.016f, 0f, false, OwnerCheatState(godMode = true))
        val player = snap.playerSnake!!
        assertTrue(player.score >= 525)
        assertEquals(4, player.kills)
        assertTrue(player.godMode)
    }

    @Test
    fun gameSettings_defaultsAndBotCountUpdates() {
        val defaults = GameSettings()
        assertTrue(defaults.showMinimap)
        assertTrue(defaults.showNameplates)
        assertTrue(defaults.showJoystickRing)
        assertTrue(defaults.soundEffectsEnabled)
        assertTrue(defaults.hapticsEnabled)

        val updated = defaults.copy(showMinimap = false, soundEffectsEnabled = false)
        assertFalse(updated.showMinimap)
        assertFalse(updated.soundEffectsEnabled)

        val engine = ArenaEngine(
            playerUid = "u2",
            playerName = "PauseTester",
            playerSkinId = "cyber_viper",
            playerPattern = SnakePattern.SOLID,
            initialBotCount = 4,
            initialDifficulty = BotDifficulty.EASY,
            isHostOrSolo = true
        )
        engine.updateBotCount(7)
        val snap = engine.step(0.016f, null, false, OwnerCheatState())
        assertEquals(7, snap.snakes.count { it.isBot })
    }

    @Test
    fun dailyReward_enforces24HourCooldownStreakAndSkinUnlocks() {
        val now = 200_000_000_000L
        val firstLogin = DailyRewardCatalog.computeStatus(0, null, now)
        assertTrue(firstLogin.canClaimNow)
        assertEquals(1, firstLogin.nextRewardDay)

        val tenHoursAgo = now - (10L * 60L * 60L * 1000L)
        val onCooldown = DailyRewardCatalog.computeStatus(1, tenHoursAgo, now)
        assertFalse(onCooldown.canClaimNow)
        assertEquals(2, onCooldown.nextRewardDay)
        assertTrue(onCooldown.remainingCooldownMs > 0L)

        val twentyFiveHoursAgo = now - (25L * 60L * 60L * 1000L)
        val day3Ready = DailyRewardCatalog.computeStatus(2, twentyFiveHoursAgo, now)
        assertTrue(day3Ready.canClaimNow)
        assertEquals(3, day3Ready.nextRewardDay)
        assertEquals(DailyRewardKind.SKIN, DailyRewardCatalog.tierForDay(3).kind)
        assertEquals("chrono_basilisk", DailyRewardCatalog.tierForDay(3).skinId)

        val fiftyHoursAgo = now - (50L * 60L * 60L * 1000L)
        val streakReset = DailyRewardCatalog.computeStatus(4, fiftyHoursAgo, now)
        assertTrue(streakReset.canClaimNow)
        assertEquals(1, streakReset.nextRewardDay)

        val day3Skin = SkinCatalog.getById("chrono_basilisk")
        assertFalse(SkinCatalog.isSkinUnlocked(day3Skin, emptyList(), isOwner = false))
        assertTrue(SkinCatalog.isSkinUnlocked(day3Skin, listOf("chrono_basilisk"), isOwner = false))
    }

    @Test
    fun arenaEngine_emitsSoundEventsForBoostAndLoot() {
        val engine = ArenaEngine(
            playerUid = "sfx_user",
            playerName = "AudioViper",
            playerSkinId = "cyber_viper",
            playerPattern = SnakePattern.SOLID,
            initialBotCount = 0,
            initialDifficulty = BotDifficulty.EASY,
            isHostOrSolo = true
        )

        val boostSnap = engine.step(0.016f, 0f, true, OwnerCheatState())
        assertTrue(SoundEffect.BOOST_START in boostSnap.soundEvents)
        assertTrue(HapticEvent.BOOST_START in boostSnap.hapticEvents)

        var sawBoostPulse = false
        repeat(20) {
            val sustained = engine.step(0.03f, 0f, true, OwnerCheatState())
            if (HapticEvent.BOOST_PULSE in sustained.hapticEvents) {
                sawBoostPulse = true
            }
        }
        assertTrue(sawBoostPulse)

        engine.spawnOwnerFoodRain()
        var sawLootSound = false
        var sawLootHaptic = false
        repeat(30) {
            val s = engine.step(0.03f, 0f, false, OwnerCheatState(permaMegaMagnet = true))
            if (SoundEffect.EAT_LOOT in s.soundEvents) {
                sawLootSound = true
            }
            if (HapticEvent.FOOD_LOOT in s.hapticEvents) {
                sawLootHaptic = true
            }
        }
        assertTrue(sawLootSound)
        assertTrue(sawLootHaptic)
    }
}
