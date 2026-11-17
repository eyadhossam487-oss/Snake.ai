package com.example.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.example.model.ActiveSnake
import com.example.model.OwnerCheatState
import com.example.model.PowerUpType
import com.example.model.SkinCatalog
import com.example.model.SnakePattern
import com.example.model.SnakeSkin
import com.example.model.Vec2
import com.example.ui.theme.ArenaBorder
import com.example.ui.theme.ArenaVoid
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SolarGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val PRISMATIC_PALETTE = listOf(
    Color(0xFF00F5D4),
    Color(0xFF4CC9F0),
    Color(0xFF9B5DE5),
    Color(0xFFFF2E93),
    Color(0xFFFFD166),
    Color(0xFF6EEB83)
)

@Composable
fun SnakePreviewCanvas(
    skinId: String,
    pattern: SnakePattern,
    modifier: Modifier = Modifier,
    showCrown: Boolean = false
) {
    val skin = remember(skinId) { SkinCatalog.getById(skinId) }
    val transition = rememberInfiniteTransition(label = "snake_preview")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "preview_wave"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val count = 16
        val startX = w * 0.80f
        val endX = w * 0.16f
        val segs = List(count) { idx ->
            val t = idx.toFloat() / (count - 1).toFloat()
            val x = startX + (endX - startX) * t
            val y = h * 0.5f + sin(phase - t * 4.5f) * (h * 0.18f)
            Vec2(x, y)
        }
        val radius = (h * 0.14f).coerceIn(11f, 22f)
        drawSnakeBody(
            segments = segs,
            headAngle = cos(phase) * 0.25f,
            radius = radius,
            skin = skin,
            pattern = pattern,
            boosting = false,
            godMode = showCrown,
            speedPuActive = false,
            magnetPuActive = false,
            pulsePhase = phase
        )
    }
}

@Composable
fun ArenaGameCanvas(
    snapshot: ArenaSnapshot,
    cameraX: Float,
    cameraY: Float,
    ownerCheats: OwnerCheatState,
    joystickCenter: Offset?,
    joystickCurrent: Offset?,
    showNameplates: Boolean = true,
    showJoystickRing: Boolean = true,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "arena_fx")
    val pulsePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = android.graphics.Color.WHITE
            setShadowLayer(6f, 0f, 0f, android.graphics.Color.BLACK)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(color = ArenaVoid)

        val viewW = size.width
        val viewH = size.height
        val halfW = viewW * 0.5f
        val halfH = viewH * 0.5f
        val offsetX = halfW - cameraX
        val offsetY = halfH - cameraY

        // 1. Cyber Arena Grid Floor
        val gridSpacing = 120f
        val minWorldX = ((cameraX - halfW) / gridSpacing).toInt() * gridSpacing
        val maxWorldX = cameraX + halfW + gridSpacing
        val minWorldY = ((cameraY - halfH) / gridSpacing).toInt() * gridSpacing
        val maxWorldY = cameraY + halfH + gridSpacing

        var gx = minWorldX
        while (gx <= maxWorldX) {
            if (gx in 0f..ArenaEngine.WORLD_SIZE) {
                val sx = gx + offsetX
                val topY = (0f + offsetY).coerceAtLeast(0f)
                val botY = (ArenaEngine.WORLD_SIZE + offsetY).coerceAtMost(viewH)
                if (botY > topY) {
                    drawLine(
                        color = ArenaBorder.copy(alpha = 0.28f),
                        start = Offset(sx, topY),
                        end = Offset(sx, botY),
                        strokeWidth = 1.5f
                    )
                }
            }
            gx += gridSpacing
        }

        var gy = minWorldY
        while (gy <= maxWorldY) {
            if (gy in 0f..ArenaEngine.WORLD_SIZE) {
                val sy = gy + offsetY
                val leftX = (0f + offsetX).coerceAtLeast(0f)
                val rightX = (ArenaEngine.WORLD_SIZE + offsetX).coerceAtMost(viewW)
                if (rightX > leftX) {
                    drawLine(
                        color = ArenaBorder.copy(alpha = 0.28f),
                        start = Offset(leftX, sy),
                        end = Offset(rightX, sy),
                        strokeWidth = 1.5f
                    )
                }
            }
            gy += gridSpacing
        }

        // 2. Arena Laser Boundary
        val bLeft = offsetX
        val bTop = offsetY
        val bSize = ArenaEngine.WORLD_SIZE
        drawRect(
            color = CrimsonDanger.copy(alpha = 0.18f),
            topLeft = Offset(bLeft - 40f, bTop - 40f),
            size = androidx.compose.ui.geometry.Size(bSize + 80f, bSize + 80f),
            style = Stroke(width = 80f)
        )
        drawRect(
            color = CrimsonDanger,
            topLeft = Offset(bLeft, bTop),
            size = androidx.compose.ui.geometry.Size(bSize, bSize),
            style = Stroke(width = 6f)
        )

        // 3. Food Orbs (culled to viewport)
        val marginCull = 60f
        for (orb in snapshot.foodOrbs) {
            val sx = orb.x + offsetX
            val sy = orb.y + offsetY
            if (sx < -marginCull || sx > viewW + marginCull || sy < -marginCull || sy > viewH + marginCull) {
                continue
            }
            val glowAlpha = if (orb.isLootDrop) 0.42f else 0.22f
            val pulse = 1f + 0.14f * sin(pulsePhase + orb.id * 0.4f)
            drawCircle(
                color = orb.color.copy(alpha = glowAlpha),
                radius = orb.radius * 2.1f * pulse,
                center = Offset(sx, sy)
            )
            drawCircle(
                color = orb.color,
                radius = orb.radius * pulse,
                center = Offset(sx, sy)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = orb.radius * 0.35f,
                center = Offset(sx - orb.radius * 0.22f, sy - orb.radius * 0.22f)
            )
        }

        // 4. Power-Ups (Speed Boost & Coin Magnet)
        for (pu in snapshot.powerUps) {
            val sx = pu.x + offsetX
            val sy = pu.y + offsetY
            if (sx < -80f || sx > viewW + 80f || sy < -80f || sy > viewH + 80f) continue

            val bob = sin(pulsePhase + pu.id) * 4f
            val center = Offset(sx, sy + bob)
            val pulseRadius = 26f + sin(pulsePhase * 1.5f) * 4f

            // Outer pulsing aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(pu.type.color.copy(alpha = 0.55f), Color.Transparent),
                    center = center,
                    radius = pulseRadius * 1.6f
                ),
                radius = pulseRadius * 1.6f,
                center = center
            )
            drawCircle(
                color = Color(0xFF0E1528),
                radius = 19f,
                center = center
            )
            drawCircle(
                color = pu.type.color,
                radius = 19f,
                center = center,
                style = Stroke(width = 3.2f)
            )

            when (pu.type) {
                PowerUpType.SPEED_BOOST -> {
                    // Lightning bolt vector inside capsule
                    val bolt = Path().apply {
                        moveTo(center.x + 2f, center.y - 11f)
                        lineTo(center.x - 7f, center.y + 1f)
                        lineTo(center.x - 1f, center.y + 1f)
                        lineTo(center.x - 3f, center.y + 11f)
                        lineTo(center.x + 7f, center.y - 1f)
                        lineTo(center.x + 1f, center.y - 1f)
                        close()
                    }
                    drawPath(path = bolt, color = pu.type.color)
                }
                PowerUpType.COIN_MAGNET -> {
                    // Horseshoe magnet arc inside capsule
                    drawArc(
                        color = pu.type.color,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(center.x - 8f, center.y - 8f),
                        size = androidx.compose.ui.geometry.Size(16f, 16f),
                        style = Stroke(width = 4.5f, cap = StrokeCap.Butt)
                    )
                    drawLine(
                        color = pu.type.accentColor,
                        start = Offset(center.x - 8f, center.y),
                        end = Offset(center.x - 8f, center.y + 7f),
                        strokeWidth = 4.5f
                    )
                    drawLine(
                        color = pu.type.accentColor,
                        start = Offset(center.x + 8f, center.y),
                        end = Offset(center.x + 8f, center.y + 7f),
                        strokeWidth = 4.5f
                    )
                }
            }
        }

        // 5. Snakes (render others first, player on top)
        val sortedSnakes = snapshot.snakes
            .filter { it.alive }
            .sortedBy { if (it.isPlayer) 1 else 0 }

        for (snake in sortedSnakes) {
            val screenSegs = snake.segments.map { Offset(it.x + offsetX, it.y + offsetY) }
            if (screenSegs.none { it.x in -120f..(viewW + 120f) && it.y in -120f..(viewH + 120f) }) {
                continue
            }
            val skin = SkinCatalog.getById(snake.skinId)
            val magnetOn = snake.magnetRemainingMs > 0L || (snake.isPlayer && ownerCheats.permaMegaMagnet)
            val speedOn = snake.speedBoostRemainingMs > 0L || (snake.isPlayer && ownerCheats.permaSpeedOverdrive)

            drawSnakeBody(
                segments = screenSegs.map { Vec2(it.x, it.y) },
                headAngle = snake.angle,
                radius = snake.bodyRadius,
                skin = skin,
                pattern = snake.pattern,
                boosting = snake.boosting,
                godMode = snake.godMode,
                speedPuActive = speedOn,
                magnetPuActive = magnetOn,
                pulsePhase = pulsePhase
            )

            // Draw nameplate above snake head
            if (showNameplates) {
                val head = screenSegs.first()
                labelPaint.color = if (snake.isPlayer) CyberCyan.toArgb() else Color.White.toArgb()
                val tagText = if (snake.isBot) "${snake.displayName} [BOT]" else snake.displayName
                drawContext.canvas.nativeCanvas.drawText(
                    tagText,
                    head.x,
                    head.y - snake.bodyRadius - 14f,
                    labelPaint
                )
            }
        }

        // 6. Floating Virtual Joystick
        if (showJoystickRing && joystickCenter != null && joystickCurrent != null) {
            drawCircle(
                color = CyberCyan.copy(alpha = 0.14f),
                radius = 68f,
                center = joystickCenter
            )
            drawCircle(
                color = CyberCyan.copy(alpha = 0.45f),
                radius = 68f,
                center = joystickCenter,
                style = Stroke(width = 2.5f)
            )
            val dx = joystickCurrent.x - joystickCenter.x
            val dy = joystickCurrent.y - joystickCenter.y
            val dist = kotlin.math.hypot(dx, dy).coerceAtLeast(1f)
            val clampedDist = dist.coerceAtMost(56f)
            val knobPos = Offset(
                joystickCenter.x + (dx / dist) * clampedDist,
                joystickCenter.y + (dy / dist) * clampedDist
            )
            drawCircle(
                color = CyberCyan.copy(alpha = 0.85f),
                radius = 24f,
                center = knobPos
            )
        }
    }
}

private fun DrawScope.drawSnakeBody(
    segments: List<Vec2>,
    headAngle: Float,
    radius: Float,
    skin: SnakeSkin,
    pattern: SnakePattern,
    boosting: Boolean,
    godMode: Boolean,
    speedPuActive: Boolean,
    magnetPuActive: Boolean,
    pulsePhase: Float
) {
    if (segments.isEmpty()) return
    val head = segments.first()

    // Magnet field pulse around head
    if (magnetPuActive) {
        val ringR = radius * 3.4f + sin(pulsePhase * 2f) * 10f
        drawCircle(
            color = SolarGold.copy(alpha = 0.25f),
            radius = ringR,
            center = Offset(head.x, head.y),
            style = Stroke(width = 3f)
        )
    }

    // God Mode / Speed aura
    if (godMode || speedPuActive || boosting) {
        val auraColor = when {
            godMode -> SolarGold
            speedPuActive -> CyberCyan
            else -> skin.glowColor
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(auraColor.copy(alpha = 0.36f), Color.Transparent),
                center = Offset(head.x, head.y),
                radius = radius * 2.5f
            ),
            radius = radius * 2.5f,
            center = Offset(head.x, head.y)
        )
    }

    // Draw segments from tail to head
    for (i in segments.indices.reversed()) {
        val seg = segments[i]
        val taper = if (i > segments.size - 5) {
            val tailFactor = (segments.size - i).toFloat() / 5f
            (0.55f + 0.45f * tailFactor)
        } else 1f
        val r = radius * taper

        val segColor = when {
            skin.isPrismatic -> {
                val idx = (i + (pulsePhase * 2f).toInt()) % PRISMATIC_PALETTE.size
                PRISMATIC_PALETTE[idx.coerceIn(0, PRISMATIC_PALETTE.lastIndex)]
            }
            pattern == SnakePattern.STRIPED -> if (i % 2 == 0) skin.primaryColor else skin.secondaryColor
            pattern == SnakePattern.DOTS -> skin.primaryColor
            else -> skin.primaryColor
        }

        if (boosting || godMode) {
            drawCircle(
                color = skin.glowColor.copy(alpha = 0.22f),
                radius = r * 1.32f,
                center = Offset(seg.x, seg.y)
            )
        }

        drawCircle(
            color = segColor,
            radius = r,
            center = Offset(seg.x, seg.y)
        )

        if (pattern == SnakePattern.NEON_CORE) {
            drawCircle(
                color = skin.secondaryColor,
                radius = r * 0.45f,
                center = Offset(seg.x, seg.y)
            )
        } else if (pattern == SnakePattern.DOTS && i % 2 == 0) {
            drawCircle(
                color = skin.headColor.copy(alpha = 0.85f),
                radius = r * 0.38f,
                center = Offset(seg.x, seg.y)
            )
        }
    }

    // Head
    drawCircle(
        color = skin.headColor,
        radius = radius * 1.12f,
        center = Offset(head.x, head.y)
    )

    // Eyes oriented along headAngle
    val eyeForward = radius * 0.46f
    val eyeSide = radius * 0.48f
    val perpAngle = headAngle + (PI.toFloat() * 0.5f)
    val leftEye = Offset(
        head.x + cos(headAngle) * eyeForward + cos(perpAngle) * eyeSide,
        head.y + sin(headAngle) * eyeForward + sin(perpAngle) * eyeSide
    )
    val rightEye = Offset(
        head.x + cos(headAngle) * eyeForward - cos(perpAngle) * eyeSide,
        head.y + sin(headAngle) * eyeForward - sin(perpAngle) * eyeSide
    )
    val eyeRadius = radius * 0.30f
    drawCircle(color = skin.eyeColor, radius = eyeRadius, center = leftEye)
    drawCircle(color = skin.eyeColor, radius = eyeRadius, center = rightEye)

    val pupilShift = eyeRadius * 0.34f
    drawCircle(
        color = Color(0xFF080B14),
        radius = eyeRadius * 0.54f,
        center = Offset(leftEye.x + cos(headAngle) * pupilShift, leftEye.y + sin(headAngle) * pupilShift)
    )
    drawCircle(
        color = Color(0xFF080B14),
        radius = eyeRadius * 0.54f,
        center = Offset(rightEye.x + cos(headAngle) * pupilShift, rightEye.y + sin(headAngle) * pupilShift)
    )

    // Sovereign Crown for God Mode / Owner Exclusive Skin
    if (godMode || skin.ownerOnly) {
        val crownCenter = Offset(head.x, head.y - radius * 1.4f)
        val crownPath = Path().apply {
            moveTo(crownCenter.x - radius * 0.65f, crownCenter.y + 4f)
            lineTo(crownCenter.x - radius * 0.65f, crownCenter.y - 7f)
            lineTo(crownCenter.x - radius * 0.25f, crownCenter.y - 1f)
            lineTo(crownCenter.x, crownCenter.y - 11f)
            lineTo(crownCenter.x + radius * 0.25f, crownCenter.y - 1f)
            lineTo(crownCenter.x + radius * 0.65f, crownCenter.y - 7f)
            lineTo(crownCenter.x + radius * 0.65f, crownCenter.y + 4f)
            close()
        }
        drawPath(path = crownPath, color = SolarGold)
    }
}
