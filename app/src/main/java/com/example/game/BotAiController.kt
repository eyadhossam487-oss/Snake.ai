package com.example.game

import com.example.model.ActiveSnake
import com.example.model.BotDifficulty
import com.example.model.FoodOrb
import com.example.model.PowerUpOrb
import com.example.model.Vec2
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

data class BotDecision(
    val targetAngle: Float,
    val shouldBoost: Boolean,
    val updatedWanderAngle: Float
)

object BotAiController {

    private val FEELER_ANGLES_BASIC = floatArrayOf(-0.65f, 0f, 0.65f)
    private val FEELER_ANGLES_ADVANCED = floatArrayOf(-1.25f, -0.75f, -0.35f, 0f, 0.35f, 0.75f, 1.25f)

    fun decide(
        bot: ActiveSnake,
        difficulty: BotDifficulty,
        allSnakes: List<ActiveSnake>,
        foodOrbs: List<FoodOrb>,
        powerUps: List<PowerUpOrb>,
        worldSize: Float
    ): BotDecision {
        val margin = when (difficulty) {
            BotDifficulty.EASY -> 140f
            BotDifficulty.MEDIUM -> 195f
            BotDifficulty.HARD -> 240f
            BotDifficulty.APEX -> 280f
        }

        // 1. Boundary Avoidance Vector
        var avoidX = 0f
        var avoidY = 0f
        var nearDanger = false

        if (bot.x < margin) {
            avoidX += ((margin - bot.x) / margin) * 3.5f
            nearDanger = true
        } else if (bot.x > worldSize - margin) {
            avoidX -= ((bot.x - (worldSize - margin)) / margin) * 3.5f
            nearDanger = true
        }
        if (bot.y < margin) {
            avoidY += ((margin - bot.y) / margin) * 3.5f
            nearDanger = true
        } else if (bot.y > worldSize - margin) {
            avoidY -= ((bot.y - (worldSize - margin)) / margin) * 3.5f
            nearDanger = true
        }

        // 2. Snake Segment Collision Avoidance (Multi-ray whisker & proximity repulsion)
        val feelerRange = difficulty.feelerRange
        val feelerAngles = if (difficulty == BotDifficulty.EASY) {
            FEELER_ANGLES_BASIC
        } else {
            FEELER_ANGLES_ADVANCED
        }

        val otherSnakes = allSnakes.filter { it.alive && it.id != bot.id }
        var closestThreatDist = Float.MAX_VALUE

        for (other in otherSnakes) {
            // Check head proximity first
            val headDx = other.x - bot.x
            val headDy = other.y - bot.y
            val headDist = hypot(headDx, headDy)
            if (headDist > feelerRange + other.segments.size * 14f) continue

            // Check segments with stride for performance
            val stride = if (difficulty == BotDifficulty.EASY) 3 else 2
            for (i in other.segments.indices step stride) {
                val seg = other.segments[i]
                val dx = seg.x - bot.x
                val dy = seg.y - bot.y
                val dist = hypot(dx, dy)
                val dangerRadius = feelerRange * 0.72f + other.bodyRadius
                if (dist < dangerRadius && dist > 1f) {
                    closestThreatDist = minOf(closestThreatDist, dist)
                    val strength = ((dangerRadius - dist) / dangerRadius).let { it * it * 4.5f }
                    avoidX -= (dx / dist) * strength
                    avoidY -= (dy / dist) * strength
                    if (dist < dangerRadius * 0.65f) {
                        nearDanger = true
                    }
                }
            }
        }

        // Evaluate candidate feeler directions to find the clearest open corridor when threatened
        var bestClearAngle = bot.angle
        if (nearDanger || closestThreatDist < feelerRange * 0.65f) {
            var bestScore = Float.NEGATIVE_INFINITY
            for (offset in feelerAngles) {
                val testAngle = normalizeAngle(bot.angle + offset)
                val probeDist = feelerRange * 0.68f
                val px = bot.x + cos(testAngle) * probeDist
                val py = bot.y + sin(testAngle) * probeDist

                var clearance = 0f
                if (px < 80f || px > worldSize - 80f || py < 80f || py > worldSize - 80f) {
                    clearance -= 400f
                }
                var minSegDist = feelerRange
                for (other in otherSnakes) {
                    for (i in other.segments.indices step 2) {
                        val d = hypot(other.segments[i].x - px, other.segments[i].y - py)
                        if (d < minSegDist) minSegDist = d
                    }
                }
                clearance += minSegDist - kotlin.math.abs(offset) * 18f
                if (clearance > bestScore) {
                    bestScore = clearance
                    bestClearAngle = testAngle
                }
            }
        }

        // 3. Food & Power-Up Pathfinding
        var seekX = 0f
        var seekY = 0f
        var foundAttractiveTarget = false
        var bestFoodScore = 0f
        var chasingRichLoot = false

        val searchRadius = when (difficulty) {
            BotDifficulty.EASY -> 340f
            BotDifficulty.MEDIUM -> 480f
            BotDifficulty.HARD -> 620f
            BotDifficulty.APEX -> 760f
        }

        // Score nearby food orbs
        for (orb in foodOrbs) {
            val dx = orb.x - bot.x
            val dy = orb.y - bot.y
            val dist = hypot(dx, dy)
            if (dist > searchRadius || dist < 1f) continue

            // Avoid chasing food that sits right next to an enemy body on Medium+
            if (difficulty != BotDifficulty.EASY && isPointNearEnemy(orb.x, orb.y, otherSnakes, 85f)) {
                continue
            }

            val lootBonus = if (orb.isLootDrop && difficulty != BotDifficulty.EASY) 2.2f else 1.0f
            val score = (orb.value * lootBonus) / (dist + 45f)
            if (score > bestFoodScore) {
                bestFoodScore = score
                seekX = dx / dist
                seekY = dy / dist
                foundAttractiveTarget = true
                chasingRichLoot = orb.isLootDrop && dist < 340f
            }
        }

        // Score nearby power-ups (Speed Boost / Coin Magnet) on Medium+
        if (difficulty != BotDifficulty.EASY) {
            for (pu in powerUps) {
                val dx = pu.x - bot.x
                val dy = pu.y - bot.y
                val dist = hypot(dx, dy)
                if (dist < searchRadius * 0.9f && dist > 1f) {
                    if (!isPointNearEnemy(pu.x, pu.y, otherSnakes, 75f)) {
                        val puScore = 14f / (dist + 45f)
                        if (puScore > bestFoodScore) {
                            bestFoodScore = puScore
                            seekX = dx / dist
                            seekY = dy / dist
                            foundAttractiveTarget = true
                            chasingRichLoot = true
                        }
                    }
                }
            }
        }

        // 4. Tactical Cut-off / Interception on Hard & Apex when safe and reasonably sized
        if (difficulty.interceptWeight > 0f && !nearDanger && bot.score >= 35) {
            var interceptTarget: ActiveSnake? = null
            var nearestDist = 420f
            for (other in otherSnakes) {
                if (other.godMode) continue
                val d = hypot(other.x - bot.x, other.y - bot.y)
                if (d in 110f..nearestDist) {
                    nearestDist = d
                    interceptTarget = other
                }
            }
            if (interceptTarget != null && Random.nextFloat() < difficulty.interceptWeight) {
                val leadX = interceptTarget.x + cos(interceptTarget.angle) * 115f
                val leadY = interceptTarget.y + sin(interceptTarget.angle) * 115f
                if (leadX in margin..(worldSize - margin) && leadY in margin..(worldSize - margin)) {
                    val dx = leadX - bot.x
                    val dy = leadY - bot.y
                    val d = hypot(dx, dy).coerceAtLeast(1f)
                    seekX = seekX * 0.35f + (dx / d) * 0.65f
                    seekY = seekY * 0.35f + (dy / d) * 0.65f
                    foundAttractiveTarget = true
                }
            }
        }

        // Wander smoothly if no food/target nearby
        var nextWander = bot.botWanderAngle
        if (!foundAttractiveTarget) {
            nextWander = normalizeAngle(bot.botWanderAngle + (Random.nextFloat() - 0.5f) * 0.65f)
            seekX = cos(nextWander)
            seekY = sin(nextWander)
        }

        // Blend avoidance and seeking
        val desiredAngle = if (hypot(avoidX, avoidY) > 0.85f) {
            val blendedX = avoidX * 1.4f + cos(bestClearAngle) * 0.9f + seekX * 0.25f
            val blendedY = avoidY * 1.4f + sin(bestClearAngle) * 0.9f + seekY * 0.25f
            atan2(blendedY, blendedX)
        } else {
            val blendedX = seekX + avoidX * 1.2f
            val blendedY = seekY + avoidY * 1.2f
            atan2(blendedY, blendedX)
        }

        val canBoost = bot.score > 22
        val shouldBoost = canBoost && (
            (nearDanger && difficulty != BotDifficulty.EASY && Random.nextFloat() < difficulty.boostChance * 1.4f) ||
                (chasingRichLoot && !nearDanger && Random.nextFloat() < difficulty.boostChance)
            )

        return BotDecision(
            targetAngle = normalizeAngle(desiredAngle),
            shouldBoost = shouldBoost,
            updatedWanderAngle = nextWander
        )
    }

    private fun isPointNearEnemy(x: Float, y: Float, enemies: List<ActiveSnake>, radius: Float): Boolean {
        for (enemy in enemies) {
            if (hypot(enemy.x - x, enemy.y - y) < radius) return true
            for (i in enemy.segments.indices step 3) {
                val seg = enemy.segments[i]
                if (hypot(seg.x - x, seg.y - y) < radius) return true
            }
        }
        return false
    }

    fun normalizeAngle(angle: Float): Float {
        var a = angle
        val twoPi = (2.0 * PI).toFloat()
        while (a > PI.toFloat()) a -= twoPi
        while (a < -PI.toFloat()) a += twoPi
        return a
    }
}
