package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.SnakePreviewCanvas
import com.example.model.ArenaRoom
import com.example.model.BotDifficulty
import com.example.model.DailyRewardCatalog
import com.example.model.DailyRewardStatus
import com.example.model.SkinCatalog
import com.example.model.SnakePattern
import com.example.model.UserProfile
import com.example.ui.theme.ArenaBorder
import com.example.ui.theme.ArenaSurface
import com.example.ui.theme.ArenaSurfaceElevated
import com.example.ui.theme.ArenaSurfaceHighlight
import com.example.ui.theme.ArenaVoid
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PlasmaMagenta
import com.example.ui.theme.SolarGold
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.UiState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    profileState: UiState<UserProfile>,
    roomsState: UiState<List<ArenaRoom>>,
    dailyRewardStatus: DailyRewardStatus,
    isOwner: Boolean,
    isBusy: Boolean,
    pausedSessionLabel: String?,
    pausedSessionLength: Int?,
    onResumePausedSession: () -> Unit,
    onEndPausedSession: () -> Unit,
    onOpenDailyRewards: () -> Unit,
    onOpenSkinStudio: () -> Unit,
    onOpenOwnerPanel: () -> Unit,
    onDifficultySelected: (BotDifficulty) -> Unit,
    onPlaySolo: (BotDifficulty, Int) -> Unit,
    onCreateRoom: (name: String, BotDifficulty, botCount: Int, maxPlayers: Int) -> Unit,
    onJoinRoom: (ArenaRoom) -> Unit,
    onJoinByCode: (String) -> Unit,
    onOwnerCloseRoom: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val profile = (profileState as? UiState.Success)?.data
    val selectedDiff = BotDifficulty.fromRaw(profile?.preferredDifficulty)
    var localDifficulty by remember(selectedDiff) { mutableStateOf(selectedDiff) }
    var botCountSlider by remember { mutableFloatStateOf(6f) }
    var joinCodeInput by remember { mutableStateOf("") }
    var showCreateRoomDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ArenaVoid),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
        ) {
            // Top Bar
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "snake.ai",
                            style = MaterialTheme.typography.headlineLarge,
                            color = CyberCyan
                        )
                        Text(
                            text = "CYBER SERPENT ARENA",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }

                    Surface(
                        color = SolarGold.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .border(1.dp, SolarGold.copy(alpha = 0.55f), RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onOpenDailyRewards)
                            .testTag("lobby_coins_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Venom Coins",
                                tint = SolarGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${profile?.coins ?: 0}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelLarge,
                                color = SolarGold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))

                    if (isOwner) {
                        Surface(
                            color = SolarGold.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .border(1.dp, SolarGold.copy(alpha = 0.7f), RoundedCornerShape(50))
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onOpenOwnerPanel)
                                .testTag("lobby_owner_panel_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = "Owner Vault",
                                    tint = SolarGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "OWNER",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SolarGold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Active Paused Session Banner (when user returned to Main Menu without leaving session)
            if (pausedSessionLabel != null) {
                item {
                    Surface(
                        color = SolarGold.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, SolarGold.copy(alpha = 0.75f), RoundedCornerShape(18.dp))
                            .testTag("paused_session_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SESSION PAUSED IN PROGRESS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SolarGold
                                )
                                Text(
                                    text = pausedSessionLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (pausedSessionLength != null) {
                                    Text(
                                        text = "Current Length: $pausedSessionLength",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                            TextButton(
                                onClick = onEndPausedSession,
                                modifier = Modifier.testTag("end_paused_session_button")
                            ) {
                                Text("END", color = CrimsonDanger)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = onResumePausedSession,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SolarGold,
                                    contentColor = ArenaVoid
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("resume_paused_session_button")
                            ) {
                                Text("RESUME", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            // Hero Snake Loadout Card
            item {
                val skinId = profile?.selectedSkinId ?: "cyber_viper"
                val pattern = SnakePattern.fromRaw(profile?.selectedPattern)
                val skin = SkinCatalog.getById(skinId)

                Surface(
                    color = ArenaSurface,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ArenaBorder, RoundedCornerShape(22.dp))
                ) {
                    Box {
                        Image(
                            painter = painterResource(id = R.drawable.img_arena_hero),
                            contentDescription = stringResource(id = R.string.hero_banner_desc),
                            contentScale = ContentScale.Crop,
                            alpha = 0.24f,
                            modifier = Modifier.matchParentSize()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            ArenaSurface.copy(alpha = 0.94f),
                                            ArenaSurface.copy(alpha = 0.72f)
                                        )
                                    )
                                )
                        )
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile?.displayName ?: "Loading...",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${skin.name} • ${pattern.label}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = skin.primaryColor
                                    )
                                }
                                OutlinedButton(
                                    onClick = onOpenSkinStudio,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("customize_skin_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = CyberCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Skins", color = CyberCyan)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            SnakePreviewCanvas(
                                skinId = skinId,
                                pattern = pattern,
                                showCrown = isOwner || skin.ownerOnly,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(78.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                StatBadge(
                                    label = "MAX LENGTH",
                                    value = "${profile?.maxLength ?: 25}",
                                    accent = CyberCyan,
                                    modifier = Modifier.weight(1f)
                                )
                                StatBadge(
                                    label = "TOTAL KILLS",
                                    value = "${profile?.totalKills ?: 0}",
                                    accent = PlasmaMagenta,
                                    modifier = Modifier.weight(1f)
                                )
                                StatBadge(
                                    label = "MATCHES",
                                    value = "${profile?.gamesPlayed ?: 0}",
                                    accent = SolarGold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Daily 24-Hour Login Reward Card
            item {
                val nextTier = DailyRewardCatalog.tierForDay(dailyRewardStatus.nextRewardDay)
                val ready = dailyRewardStatus.canClaimNow
                Surface(
                    color = if (ready) CyberCyan.copy(alpha = 0.12f) else ArenaSurface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (ready) 1.5.dp else 1.dp,
                            color = if (ready) CyberCyan else ArenaBorder,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onOpenDailyRewards)
                        .testTag("daily_rewards_banner")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Surface(
                            color = (if (ready) CyberCyan else SolarGold).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    tint = if (ready) CyberCyan else SolarGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (ready) {
                                    "DAILY REWARD READY • DAY ${nextTier.day}/7"
                                } else {
                                    "DAILY LOGIN STREAK • ${dailyRewardStatus.currentStreak}/7"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (ready) CyberCyan else SolarGold
                            )
                            Text(
                                text = if (nextTier.skinId != null) {
                                    "Day ${nextTier.day}: ${nextTier.title} Skin + ${nextTier.coinsAmount} Coins"
                                } else {
                                    "Day ${nextTier.day}: +${nextTier.coinsAmount} Venom Coins"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!ready) {
                                Text(
                                    text = "Next in ${DailyRewardCatalog.formatCountdown(dailyRewardStatus.remainingCooldownMs)}",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onOpenDailyRewards,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (ready) CyberCyan else ArenaSurfaceElevated,
                                contentColor = if (ready) ArenaVoid else CyberCyan
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("open_daily_rewards_button")
                        ) {
                            Text(
                                text = if (ready) "CLAIM" else "VIEW",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Solo vs AI Bots Card
            item {
                Surface(
                    color = ArenaSurface,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ArenaBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "QUICK BATTLE VS AI BOTS",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Bot AI Difficulty",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BotDifficulty.entries.forEach { diff ->
                                val selected = localDifficulty == diff
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        localDifficulty = diff
                                        onDifficultySelected(diff)
                                    },
                                    label = { Text(diff.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                                        selectedLabelColor = CyberCyan
                                    ),
                                    modifier = Modifier.testTag("difficulty_chip_${diff.name.lowercase()}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = localDifficulty.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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
                                text = "${botCountSlider.roundToInt()} Bots",
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                        }
                        Slider(
                            value = botCountSlider,
                            onValueChange = { botCountSlider = it },
                            valueRange = 0f..12f,
                            steps = 11,
                            colors = SliderDefaults.colors(
                                thumbColor = CyberCyan,
                                activeTrackColor = CyberCyan
                            ),
                            modifier = Modifier.testTag("bot_count_slider")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { onPlaySolo(localDifficulty, botCountSlider.roundToInt()) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberCyan,
                                contentColor = ArenaVoid
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("enter_solo_arena_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ENTER ARENA",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Multiplayer Rooms Header + Join By Code
            item {
                Surface(
                    color = ArenaSurface,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ArenaBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = PlasmaMagenta,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "MULTIPLAYER ARENAS",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Button(
                                onClick = { showCreateRoomDialog = true },
                                enabled = !isBusy,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PlasmaMagenta,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("create_room_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Host", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = joinCodeInput,
                                onValueChange = { if (it.length <= 8) joinCodeInput = it.uppercase() },
                                label = { Text("Room Code") },
                                placeholder = { Text("VIPER1") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("join_code_input")
                            )
                            Button(
                                onClick = { onJoinByCode(joinCodeInput) },
                                enabled = joinCodeInput.trim().length >= 4 && !isBusy,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("join_by_code_button")
                            ) {
                                Text("JOIN")
                            }
                        }
                    }
                }
            }

            // Live Open Rooms List
            when (roomsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            CircularProgressIndicator(color = CyberCyan)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Surface(
                            color = CrimsonDanger.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = roomsState.message,
                                color = CrimsonDanger,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    val rooms = roomsState.data
                    if (rooms.isEmpty()) {
                        item {
                            Surface(
                                color = ArenaSurfaceElevated,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, ArenaBorder, RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "No live rooms yet — tap Host to start a multiplayer battle!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        items(rooms, key = { it.id }) { room ->
                            RoomRowCard(
                                room = room,
                                isOwner = isOwner,
                                isBusy = isBusy,
                                onJoin = { onJoinRoom(room) },
                                onOwnerClose = { onOwnerCloseRoom(room.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateRoomDialog) {
        CreateRoomDialog(
            initialDifficulty = localDifficulty,
            onDismiss = { showCreateRoomDialog = false },
            onConfirm = { name, diff, bots, maxPlayers ->
                showCreateRoomDialog = false
                onCreateRoom(name, diff, bots, maxPlayers)
            }
        )
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ArenaSurfaceHighlight.copy(alpha = 0.72f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, ArenaBorder, RoundedCornerShape(12.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
        ) {
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = accent
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun RoomRowCard(
    room: ArenaRoom,
    isOwner: Boolean,
    isBusy: Boolean,
    onJoin: () -> Unit,
    onOwnerClose: () -> Unit
) {
    Surface(
        color = ArenaSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ArenaBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PlasmaMagenta.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Groups,
                    contentDescription = null,
                    tint = PlasmaMagenta
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Code ${room.roomCode} • ${room.playerCount}/${room.maxPlayers} players • ${room.botCount} ${room.botDifficulty} bots",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isOwner) {
                IconButton(
                    onClick = onOwnerClose,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Owner Close Room",
                        tint = CrimsonDanger
                    )
                }
            }
            Button(
                onClick = onJoin,
                enabled = !isBusy && room.playerCount < room.maxPlayers,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("join_room_${room.id}")
            ) {
                Text("JOIN")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateRoomDialog(
    initialDifficulty: BotDifficulty,
    onDismiss: () -> Unit,
    onConfirm: (name: String, BotDifficulty, botCount: Int, maxPlayers: Int) -> Unit
) {
    var roomName by remember { mutableStateOf("Cyber Grid Arena") }
    var difficulty by remember { mutableStateOf(initialDifficulty) }
    var botCount by remember { mutableFloatStateOf(6f) }
    var maxPlayers by remember { mutableFloatStateOf(8f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ArenaSurface,
        title = {
            Text("Host Multiplayer Arena", style = MaterialTheme.typography.headlineMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { if (it.length <= 32) roomName = it },
                    label = { Text("Arena Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_room_name_input")
                )
                Text("Bot AI Difficulty", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BotDifficulty.entries.forEach { diff ->
                        FilterChip(
                            selected = difficulty == diff,
                            onClick = { difficulty = diff },
                            label = { Text(diff.label) }
                        )
                    }
                }
                Text("AI Bots: ${botCount.roundToInt()}", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = botCount,
                    onValueChange = { botCount = it },
                    valueRange = 0f..12f,
                    steps = 11
                )
                Text("Max Players: ${maxPlayers.roundToInt()}", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = maxPlayers,
                    onValueChange = { maxPlayers = it },
                    valueRange = 2f..12f,
                    steps = 9
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        roomName,
                        difficulty,
                        botCount.roundToInt(),
                        maxPlayers.roundToInt()
                    )
                },
                modifier = Modifier.testTag("confirm_create_room_button")
            ) {
                Text("LAUNCH ROOM")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
