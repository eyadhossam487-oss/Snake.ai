package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.game.ArenaEngine
import com.example.game.ArenaGameCanvas
import com.example.game.ArenaSnapshot
import com.example.model.ArenaRoom
import com.example.model.BotDifficulty
import com.example.model.GameSettings
import com.example.model.MatchSummary
import com.example.model.OwnerCheatState
import com.example.model.PowerUpType
import com.example.ui.theme.ArenaBorder
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.ArenaSurfaceElevated
import com.example.ui.theme.ArenaVoid
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PlasmaMagenta
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextSecondary
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

private enum class PauseOverlayPage {
    MENU,
    SETTINGS
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArenaScreen(
    snapshot: ArenaSnapshot?,
    cameraPos: Pair<Float, Float>,
    room: ArenaRoom?,
    activeDifficulty: BotDifficulty,
    activeBotCount: Int,
    gameSettings: GameSettings,
    isOwner: Boolean,
    ownerCheats: OwnerCheatState,
    isPaused: Boolean,
    matchSummary: MatchSummary?,
    isHostOrSolo: Boolean,
    onSteerAngle: (Float?) -> Unit,
    onBoostChange: (Boolean) -> Unit,
    onTogglePause: (Boolean) -> Unit,
    onUpdateSettings: ((GameSettings) -> GameSettings) -> Unit,
    onDifficultyChange: (BotDifficulty) -> Unit,
    onBotCountChange: (Int) -> Unit,
    onRespawn: () -> Unit,
    onReturnToMainMenuKeepSession: () -> Unit,
    onExitArena: () -> Unit,
    onOpenOwnerPanel: () -> Unit,
    onPlayClickSound: () -> Unit = {}
) {
    var pausePage by remember(isPaused) { mutableStateOf(PauseOverlayPage.MENU) }

    BackHandler {
        when {
            matchSummary != null -> onExitArena()
            isPaused && pausePage == PauseOverlayPage.SETTINGS -> pausePage = PauseOverlayPage.MENU
            else -> onTogglePause(!isPaused)
        }
    }

    val haptics = LocalHapticFeedback.current

    var joystickCenter by remember { mutableStateOf<Offset?>(null) }
    var joystickCurrent by remember { mutableStateOf<Offset?>(null) }
    var boostPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaVoid)
    ) {
        // 1. Live 60 FPS Game Canvas with Touch Steering
        if (snapshot != null) {
            ArenaGameCanvas(
                snapshot = snapshot,
                cameraX = cameraPos.first,
                cameraY = cameraPos.second,
                ownerCheats = ownerCheats,
                joystickCenter = joystickCenter,
                joystickCurrent = joystickCurrent,
                showNameplates = gameSettings.showNameplates,
                showJoystickRing = gameSettings.showJoystickRing,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isPaused) {
                        if (isPaused) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            joystickCenter = down.position
                            joystickCurrent = down.position
                            val pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId }
                                if (change == null || !change.pressed) {
                                    joystickCenter = null
                                    joystickCurrent = null
                                    onSteerAngle(null)
                                    break
                                }
                                change.consume()
                                joystickCurrent = change.position
                                val dx = change.position.x - (joystickCenter?.x ?: change.position.x)
                                val dy = change.position.y - (joystickCenter?.y ?: change.position.y)
                                if (hypot(dx, dy) > 10f) {
                                    onSteerAngle(atan2(dy, dx))
                                }
                            }
                        }
                    }
            )
        }

        // 2. Top Safe-Area HUD
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp)
        ) {
            // Top-Left: Pause Button, Score, Rank, Kills, Room Code, Active Power-Up Timers
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = ArenaSurface.copy(alpha = 0.86f),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.dp, ArenaBorder, CircleShape)
                    ) {
                        IconButton(
                            onClick = {
                                onPlayClickSound()
                                onTogglePause(true)
                            },
                            modifier = Modifier.testTag("arena_pause_button")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause Arena")
                        }
                    }

                    val player = snapshot?.playerSnake
                    Surface(
                        color = ArenaSurface.copy(alpha = 0.86f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.border(1.dp, ArenaBorder, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Text("LEN", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = "${player?.score ?: 25}",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                            }
                            Column {
                                Text("KILLS", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = "${player?.kills ?: 0}",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = PlasmaMagenta
                                )
                            }
                            Column {
                                Text("RANK", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = "#${snapshot?.playerRank ?: 1}/${snapshot?.totalAliveCount ?: 1}",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = SolarGold
                                )
                            }
                        }
                    }
                }

                if (room != null) {
                    Surface(
                        color = ArenaSurface.copy(alpha = 0.84f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.border(1.dp, PlasmaMagenta.copy(alpha = 0.5f), RoundedCornerShape(50))
                    ) {
                        Text(
                            text = "${room.name} • CODE ${room.roomCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PlasmaMagenta,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Active Power-Up Timers (Speed Boost & Coin Magnet)
                val player = snapshot?.playerSnake
                if (player != null) {
                    if (player.speedBoostRemainingMs > 0L) {
                        PowerUpTimerBadge(
                            label = "SPEED BOOST",
                            remainingMs = player.speedBoostRemainingMs,
                            totalMs = PowerUpType.SPEED_BOOST.durationMs,
                            color = PowerUpType.SPEED_BOOST.color,
                            icon = Icons.Default.Bolt
                        )
                    }
                    if (player.magnetRemainingMs > 0L) {
                        PowerUpTimerBadge(
                            label = "COIN MAGNET",
                            remainingMs = player.magnetRemainingMs,
                            totalMs = PowerUpType.COIN_MAGNET.durationMs,
                            color = PowerUpType.COIN_MAGNET.color,
                            icon = Icons.Default.AutoAwesome
                        )
                    }
                }
            }

            // Top-Right: Live Mini Leaderboard + Owner Cheat Button
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                if (isOwner) {
                    Surface(
                        color = SolarGold.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .border(1.5.dp, SolarGold, RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpenOwnerPanel)
                            .testTag("arena_owner_panel_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (ownerCheats.godMode) Icons.Default.Shield else Icons.Default.WorkspacePremium,
                                contentDescription = "Owner Cheats",
                                tint = SolarGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (ownerCheats.godMode) "GOD MODE" else "OWNER",
                                style = MaterialTheme.typography.labelLarge,
                                color = SolarGold
                            )
                        }
                    }
                }

                if (snapshot != null && snapshot.hudLeaderboard.isNotEmpty()) {
                    Surface(
                        color = ArenaSurface.copy(alpha = 0.82f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .widthIn(min = 148.dp, max = 188.dp)
                            .border(1.dp, ArenaBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "ARENA TOP",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            snapshot.hudLeaderboard.take(4).forEachIndexed { idx, entry ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${idx + 1}.",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (idx == 0) SolarGold else TextSecondary,
                                        modifier = Modifier.width(18.dp)
                                    )
                                    Text(
                                        text = entry.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (entry.isPlayer) CyberCyan else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${entry.length}",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (entry.isPlayer) CyberCyan else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Center-Top Event Toast Banner
            AnimatedVisibility(
                visible = snapshot?.recentBannerEvent != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
            ) {
                Surface(
                    color = ArenaSurfaceElevated.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.border(1.dp, CyberCyan.copy(alpha = 0.7f), RoundedCornerShape(50))
                ) {
                    Text(
                        text = snapshot?.recentBannerEvent.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        color = CyberCyan,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                    )
                }
            }

            // Bottom-Left: Live Radar Minimap (controlled by GameSettings.showMinimap)
            if (gameSettings.showMinimap && snapshot != null) {
                Surface(
                    color = ArenaSurface.copy(alpha = 0.80f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(96.dp)
                        .border(1.dp, ArenaBorder, RoundedCornerShape(14.dp))
                        .testTag("arena_minimap")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                        val scaleX = size.width / ArenaEngine.WORLD_SIZE
                        val scaleY = size.height / ArenaEngine.WORLD_SIZE

                        for (pu in snapshot.powerUps) {
                            drawCircle(
                                color = pu.type.color,
                                radius = 2.5f,
                                center = Offset(pu.x * scaleX, pu.y * scaleY)
                            )
                        }
                        for (s in snapshot.snakes) {
                            if (!s.alive) continue
                            val color = when {
                                s.isPlayer -> CyberCyan
                                s.isBot -> PlasmaMagenta.copy(alpha = 0.85f)
                                else -> SolarGold
                            }
                            drawCircle(
                                color = color,
                                radius = if (s.isPlayer) 4.5f else 3f,
                                center = Offset(s.x * scaleX, s.y * scaleY)
                            )
                        }
                    }
                }
            }

            // Bottom-Right: Tactile Hold-to-Boost Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(82.dp)
                    .clip(CircleShape)
                    .background(
                        if (boostPressed) CyberCyan else ArenaSurface.copy(alpha = 0.86f)
                    )
                    .border(
                        width = 2.dp,
                        color = if (boostPressed) Color.White else CyberCyan,
                        shape = CircleShape
                    )
                    .pointerInput(isPaused) {
                        if (isPaused) return@pointerInput
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            boostPressed = true
                            onBoostChange(true)
                            if (gameSettings.hapticsEnabled) {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.none { it.pressed }) {
                                    boostPressed = false
                                    onBoostChange(false)
                                    break
                                }
                            }
                        }
                    }
                    .testTag("arena_boost_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Turbo Boost",
                        tint = if (boostPressed) ArenaVoid else CyberCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "BOOST",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (boostPressed) ArenaVoid else CyberCyan
                    )
                }
            }
        }

        // 3. Full Overlay Pause Menu (stops game logic, provides Settings & Return to Main Menu keeping session)
        if (isPaused && matchSummary == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(ArenaVoid.copy(alpha = 0.80f))
                    .pointerInput(Unit) {
                        // Consume background touches while paused so arena doesn't steer underneath
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                        }
                    }
                    .padding(20.dp)
                    .testTag("pause_menu_overlay")
            ) {
                Surface(
                    color = ArenaSurface,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                        .border(1.5.dp, CyberCyan.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(22.dp)
                    ) {
                        when (pausePage) {
                            PauseOverlayPage.MENU -> {
                                Text(
                                    text = "GAME PAUSED",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = CyberCyan
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (room != null) {
                                        "${room.name} (${room.roomCode}) • Simulation Halted"
                                    } else {
                                        "Solo AI Arena • Simulation Halted"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                val player = snapshot?.playerSnake
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    SummaryStat("LENGTH", "${player?.score ?: 25}", CyberCyan, Modifier.weight(1f))
                                    SummaryStat("KILLS", "${player?.kills ?: 0}", PlasmaMagenta, Modifier.weight(1f))
                                    SummaryStat("RANK", "#${snapshot?.playerRank ?: 1}", SolarGold, Modifier.weight(1f))
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Button(
                                    onClick = {
                                        onPlayClickSound()
                                        onTogglePause(false)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyberCyan,
                                        contentColor = ArenaVoid
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("resume_arena_button")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("RESUME GAME", style = MaterialTheme.typography.labelLarge)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = {
                                        onPlayClickSound()
                                        pausePage = PauseOverlayPage.SETTINGS
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("pause_settings_button")
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, tint = CyberCyan)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SETTINGS", color = MaterialTheme.colorScheme.onSurface)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = {
                                        onPlayClickSound()
                                        onReturnToMainMenuKeepSession()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("pause_main_menu_keep_session_button")
                                ) {
                                    Icon(Icons.Default.Home, contentDescription = null, tint = SolarGold)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("MAIN MENU (KEEP SESSION)", color = SolarGold)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = ArenaBorder)
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = {
                                        onPlayClickSound()
                                        onExitArena()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("exit_arena_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = CrimsonDanger)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("END & LEAVE SESSION", color = CrimsonDanger)
                                }
                            }

                            PauseOverlayPage.SETTINGS -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = {
                                            onPlayClickSound()
                                            pausePage = PauseOverlayPage.MENU
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("settings_back_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back to Pause Menu",
                                            tint = CyberCyan
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "ARENA SETTINGS",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = CyberCyan
                                        )
                                        Text(
                                            text = "Changes apply immediately on resume",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                SettingsSwitchRow(
                                    title = "Radar Minimap",
                                    subtitle = "Display live arena radar in bottom-left",
                                    checked = gameSettings.showMinimap,
                                    testTag = "setting_minimap_switch",
                                    onCheckedChange = { v -> onUpdateSettings { it.copy(showMinimap = v) } }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                SettingsSwitchRow(
                                    title = "Snake Nameplates",
                                    subtitle = "Show player & bot callsigns above snakes",
                                    checked = gameSettings.showNameplates,
                                    testTag = "setting_nameplates_switch",
                                    onCheckedChange = { v -> onUpdateSettings { it.copy(showNameplates = v) } }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                SettingsSwitchRow(
                                    title = "Virtual Joystick Ring",
                                    subtitle = "Show floating steering ring when dragging",
                                    checked = gameSettings.showJoystickRing,
                                    testTag = "setting_joystick_switch",
                                    onCheckedChange = { v -> onUpdateSettings { it.copy(showJoystickRing = v) } }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                SettingsSwitchRow(
                                    title = "Sound Effects",
                                    subtitle = "Play arcade audio for orbs, boosts & hits",
                                    checked = gameSettings.soundEffectsEnabled,
                                    testTag = "setting_sfx_switch",
                                    onCheckedChange = { v -> onUpdateSettings { it.copy(soundEffectsEnabled = v) } }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                SettingsSwitchRow(
                                    title = "Haptic Feedback",
                                    subtitle = "Subtle vibration on food pickups, boosts & hits",
                                    checked = gameSettings.hapticsEnabled,
                                    testTag = "setting_haptics_switch",
                                    onCheckedChange = { v -> onUpdateSettings { it.copy(hapticsEnabled = v) } }
                                )

                                if (isHostOrSolo) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = ArenaBorder)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "BOT AI DIFFICULTY",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextSecondary,
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        BotDifficulty.entries.forEach { diff ->
                                            FilterChip(
                                                selected = activeDifficulty == diff,
                                                onClick = { onDifficultyChange(diff) },
                                                label = { Text(diff.label) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                                                    selectedLabelColor = CyberCyan
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "AI Bot Count",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "$activeBotCount Bots",
                                            fontFamily = JetBrainsMonoFamily,
                                            color = CyberCyan
                                        )
                                    }
                                    Slider(
                                        value = activeBotCount.toFloat(),
                                        onValueChange = { onBotCountChange(it.roundToInt()) },
                                        valueRange = 0f..12f,
                                        steps = 11,
                                        colors = SliderDefaults.colors(
                                            thumbColor = CyberCyan,
                                            activeTrackColor = CyberCyan
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        onPlayClickSound()
                                        pausePage = PauseOverlayPage.MENU
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyberCyan,
                                        contentColor = ArenaVoid
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("settings_done_button")
                                ) {
                                    Text("DONE", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Match Summary / Eliminated Overlay
        if (matchSummary != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(ArenaVoid.copy(alpha = 0.84f))
                    .padding(24.dp)
            ) {
                Surface(
                    color = ArenaSurface,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 420.dp)
                        .border(1.5.dp, PlasmaMagenta.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "ELIMINATED",
                            style = MaterialTheme.typography.headlineLarge,
                            color = CrimsonDanger
                        )
                        Text(
                            text = "Stopped by ${matchSummary.eliminatedBy}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        if (matchSummary.isNewRecord) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = SolarGold.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = SolarGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "NEW PERSONAL BEST!",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SolarGold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SummaryStat("FINAL LEN", "${matchSummary.finalLength}", CyberCyan, Modifier.weight(1f))
                            SummaryStat("PEAK RANK", "#${matchSummary.peakRank}", SolarGold, Modifier.weight(1f))
                            SummaryStat("KILLS", "${matchSummary.kills}", PlasmaMagenta, Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                onPlayClickSound()
                                onRespawn()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = ArenaVoid
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("respawn_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PLAY AGAIN", style = MaterialTheme.typography.labelLarge)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                onPlayClickSound()
                                onExitArena()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("summary_exit_lobby_button")
                        ) {
                            Text("RETURN TO LOBBY")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = ArenaSurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ArenaBorder, RoundedCornerShape(14.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ArenaVoid,
                    checkedTrackColor = CyberCyan
                ),
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

@Composable
private fun PowerUpTimerBadge(
    label: String,
    remainingMs: Long,
    totalMs: Long,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val progress = (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    val secs = String.format("%.1fs", remainingMs / 1000f)

    Surface(
        color = ArenaSurface.copy(alpha = 0.88f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(156.dp)
            .border(1.dp, color.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = color)
                }
                Text(secs, fontFamily = JetBrainsMonoFamily, style = MaterialTheme.typography.labelSmall, color = color)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = color,
                trackColor = ArenaSurfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun SummaryStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ArenaSurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, ArenaBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}
